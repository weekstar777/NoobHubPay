import win32gui
import win32con
import win32ui
from PIL import Image, ImageEnhance
import numpy as np
import mss
import mss.tools
import time
import logging
import json
import re
import requests
from typing import Dict, Optional, Tuple, List
from dataclasses import dataclass

@dataclass
class PaymentInfo:
    method: str
    player: str
    product: str
    amount: float
    regions_data: Dict[str, str]  # 各区域识别的原始数据

class WindowMonitor:
    def __init__(self, config_path="config.json"):
        with open(config_path, 'r', encoding='utf-8') as f:
            self.config = json.load(f)

        # 初始化日志
        logging.basicConfig(
            level=getattr(logging, self.config['log']['level']),
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(self.config['log']['file'], encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)

        # 初始化OCR
        self.ocr_reader = None
        self._init_ocr()

        # 已处理的记录(防重复)
        self.processed_payments = set()

        # 加载商品列表
        self.products = [
            {'name': '64个钻石块', 'price': 68.00},
            {'name': '1月VIP会员', 'price': 68.00},
            {'name': '铁套装', 'price': 50.00},
            {'name': '32个金苹果', 'price': 100.00},
        ]

        self.logger.info("WindowMonitor initialized")

    def _init_ocr(self):
        """初始化OCR引擎"""
        try:
            engine = self.config['ocr'].get('engine', 'easyocr')

            if engine == 'paddleocr':
                from paddleocr import PaddleOCR
                self.ocr_reader = PaddleOCR(lang=self.config['ocr'].get('lang', 'ch'))
                self.logger.info(f"PaddleOCR initialized")
            elif engine == 'easyocr':
                import easyocr
                self.ocr_reader = easyocr.Reader(
                    self.config['ocr'].get('languages', ['ch_sim', 'en']),
                    gpu=False
                )
                self.logger.info(f"EasyOCR initialized")
            else:
                raise ValueError(f"Unknown OCR engine: {engine}")

            self.ocr_engine = engine
        except Exception as e:
            self.logger.error(f"Failed to initialize OCR: {e}")
            raise

    def find_window(self, title: str) -> Optional[int]:
        """查找窗口句柄"""
        hwnd = win32gui.FindWindow(None, title)
        if hwnd == 0:
            return None
        return hwnd

    def capture_region(self, hwnd: int, region: Dict) -> Optional[Image.Image]:
        """截取窗口内的特定区域"""
        try:
            # 获取窗口位置
            rect = win32gui.GetWindowRect(hwnd)
            win_x, win_y, _, _ = rect

            # 计算区域在屏幕上的绝对坐标
            abs_x = win_x + region['x']
            abs_y = win_y + region['y']
            width = region['width']
            height = region['height']

            # 使用mss截图
            with mss.MSS() as sct:
                monitor = {
                    "top": abs_y,
                    "left": abs_x,
                    "width": width,
                    "height": height
                }
                screenshot = sct.grab(monitor)
                img = Image.frombytes('RGB', screenshot.size, screenshot.rgb)

            # 图像预处理：放大2倍+增强对比度
            img = img.resize((width * 2, height * 2), Image.LANCZOS)
            enhancer = ImageEnhance.Contrast(img)
            img = enhancer.enhance(1.5)

            return img
        except Exception as e:
            self.logger.error(f"Failed to capture region: {e}")
            return None

    def ocr_image(self, img: Image.Image) -> str:
        """对图像进行OCR识别"""
        try:
            img_array = np.array(img)

            if self.ocr_engine == 'paddleocr':
                result = self.ocr_reader.predict(img_array)
                texts = []
                if result and len(result) > 0:
                    for line in result[0]:
                        if len(line) >= 2:
                            texts.append(line[1][0])
                return ' '.join(texts)
            elif self.ocr_engine == 'easyocr':
                result = self.ocr_reader.readtext(img_array)
                confidence_threshold = self.config['ocr'].get('confidence_threshold', 0.3)
                texts = [text for _, text, conf in result if conf >= confidence_threshold]
                return ' '.join(texts)
        except Exception as e:
            self.logger.error(f"OCR failed: {e}")
            return ""

    def recognize_regions(self, hwnd: int, regions: Dict[str, Dict]) -> Dict[str, str]:
        """识别所有配置的区域"""
        results = {}
        for region_name, region_config in regions.items():
            img = self.capture_region(hwnd, region_config)
            if img:
                text = self.ocr_image(img)
                results[region_name] = text.strip()
                self.logger.info(f"Region '{region_name}': {text.strip()}")
            else:
                results[region_name] = ""
        return results

    def match_product(self, product_text: str) -> Optional[Dict]:
        """根据收款项文本匹配商品"""
        # 标准化文本：去除空格和特殊字符
        normalized = re.sub(r'[^\w一-龥]', '', product_text)

        for product in self.products:
            product_normalized = re.sub(r'[^\w一-龥]', '', product['name'])
            if product_normalized in normalized or normalized in product_normalized:
                self.logger.info(f"Matched product: {product['name']}")
                return product
        return None

    def extract_amount(self, amount_text: str) -> Optional[float]:
        """从金额文本中提取数值"""
        # 尝试各种金额格式
        patterns = [
            r'¥?\s*(\d+\.\d{2})',  # ¥68.00 或 68.00
            r'(\d+\.\d+)\s*元',     # 68.00元
            r'(\d+)',               # 纯数字
        ]

        for pattern in patterns:
            match = re.search(pattern, amount_text)
            if match:
                return float(match.group(1))
        return None

    def parse_payment_info(self, regions_data: Dict[str, str], method: str) -> Optional[PaymentInfo]:
        """解析支付信息"""
        try:
            # 1. 匹配商品
            product_text = regions_data.get('收款项', '')
            product = self.match_product(product_text)
            if not product:
                self.logger.warning(f"No product matched for: {product_text}")
                return None

            # 2. 提取金额
            amount_text = regions_data.get('收款金额', '')
            amount = self.extract_amount(amount_text)
            if not amount:
                self.logger.warning(f"No amount extracted from: {amount_text}")
                return None

            # 3. 验证金额与商品价格一致
            if abs(amount - product['price']) > 0.01:
                self.logger.warning(f"Amount mismatch: {amount} vs {product['price']}")
                return None

            # 4. 提取玩家名（原始文本，服务器端会匹配已注册玩家）
            player_text = regions_data.get('付款方信息', '').strip()
            if not player_text:
                self.logger.warning("No player info found")
                return None

            return PaymentInfo(
                method=method,
                player=player_text,
                product=product['name'],
                amount=amount,
                regions_data=regions_data
            )
        except Exception as e:
            self.logger.error(f"Failed to parse payment info: {e}")
            return None

    def send_to_server(self, payment_info: PaymentInfo) -> bool:
        """发送支付信息到服务器"""
        try:
            data = {
                'method': payment_info.method,
                'player': payment_info.player,
                'product': payment_info.product,
                'amount': payment_info.amount,
                'regions_data': payment_info.regions_data
            }

            response = requests.post(
                self.config['http_server']['url'],
                json=data,
                headers={'X-Secret-Key': self.config['http_server']['secret_key']},
                timeout=5
            )

            if response.status_code == 200:
                self.logger.info(f"Payment sent successfully: {payment_info.player} - {payment_info.product}")
                return True
            else:
                self.logger.error(f"Server error: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.logger.error(f"Failed to send payment: {e}")
            return False

    def check_window(self, method: str, window_title: str, regions: Dict[str, Dict]):
        """检查指定窗口"""
        hwnd = self.find_window(window_title)
        if not hwnd:
            return

        # 识别各区域
        regions_data = self.recognize_regions(hwnd, regions)

        # 解析支付信息
        payment_info = self.parse_payment_info(regions_data, method)
        if not payment_info:
            return

        # 防重复：生成唯一标识
        payment_id = f"{payment_info.method}_{payment_info.player}_{payment_info.product}_{payment_info.amount}"
        if payment_id in self.processed_payments:
            return

        # 发送到服务器
        if self.send_to_server(payment_info):
            self.processed_payments.add(payment_id)

    def run(self):
        """主循环"""
        self.logger.info("Monitor started")

        while True:
            try:
                # 检查微信支付
                if self.config['wechat']['enabled']:
                    self.check_window(
                        'wechat',
                        self.config['wechat']['window_title'],
                        self.config['wechat']['regions']
                    )

                # 检查支付宝
                if self.config['alipay']['enabled']:
                    self.check_window(
                        'alipay',
                        self.config['alipay']['window_title'],
                        self.config['alipay']['regions']
                    )

                # 使用最小的检查间隔
                intervals = []
                if self.config['wechat']['enabled']:
                    intervals.append(self.config['wechat']['check_interval'])
                if self.config['alipay']['enabled']:
                    intervals.append(self.config['alipay']['check_interval'])

                time.sleep(min(intervals) if intervals else 2.0)

            except KeyboardInterrupt:
                self.logger.info("Monitor stopped by user")
                break
            except Exception as e:
                self.logger.error(f"Error in main loop: {e}")
                time.sleep(5)

if __name__ == "__main__":
    monitor = WindowMonitor()
    monitor.run()

