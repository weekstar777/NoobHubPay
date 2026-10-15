import win32gui
import win32con
import win32ui
from PIL import Image, ImageEnhance, ImageDraw, ImageFont
import numpy as np
import mss
import mss.tools
import time
import logging
import json
import re
import requests
import os
from typing import Dict, Optional, Tuple, List
from dataclasses import dataclass

@dataclass
class PaymentInfo:
    method: str
    player: str
    amount: float
    regions_data: Dict[str, str]

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

        # 订单序号跟踪（防重复）
        self.last_order_number = {}  # {method: 最后处理的序号}

        # 调试模式：保存截图
        self.debug_mode = self.config.get('debug', {}).get('enabled', False)
        self.debug_dir = self.config.get('debug', {}).get('output_dir', 'debug_screenshots')
        if self.debug_mode and not os.path.exists(self.debug_dir):
            os.makedirs(self.debug_dir)

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

    def capture_region(self, hwnd: int, region: Dict, region_name: str = "") -> Optional[Image.Image]:
        """截取窗口内的特定区域（相对于客户区）"""
        try:
            # 获取客户区左上角在屏幕上的坐标
            client_rect = win32gui.GetClientRect(hwnd)
            client_left_top = win32gui.ClientToScreen(hwnd, (client_rect[0], client_rect[1]))

            # 计算区域在屏幕上的绝对坐标（相对于客户区）
            abs_x = client_left_top[0] + region['x']
            abs_y = client_left_top[1] + region['y']
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

            # 调试模式：保存原始截图
            if self.debug_mode and region_name:
                timestamp = time.strftime("%Y%m%d_%H%M%S")
                debug_path = os.path.join(self.debug_dir, f"{timestamp}_{region_name}_raw.png")
                img.save(debug_path)
                self.logger.info(f"Debug: saved raw screenshot to {debug_path}")

            # 图像预处理：放大2倍+增强对比度
            img = img.resize((width * 2, height * 2), Image.LANCZOS)
            enhancer = ImageEnhance.Contrast(img)
            img = enhancer.enhance(1.5)

            # 调试模式：保存预处理后的截图
            if self.debug_mode and region_name:
                timestamp = time.strftime("%Y%m%d_%H%M%S")
                debug_path = os.path.join(self.debug_dir, f"{timestamp}_{region_name}_processed.png")
                img.save(debug_path)
                self.logger.info(f"Debug: saved processed screenshot to {debug_path}")

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

    def extract_order_number(self, summary_text: str) -> Optional[int]:
        """从汇总文本中提取订单序号（今日第x笔）"""
        patterns = [
            r'第\s*(\d+)\s*笔',
            r'第(\d+)笔',
        ]

        for pattern in patterns:
            match = re.search(pattern, summary_text)
            if match:
                return int(match.group(1))
        return None

    def recognize_regions(self, hwnd: int, regions: Dict[str, Dict]) -> Dict[str, str]:
        """识别所有配置的区域"""
        results = {}
        for region_name, region_config in regions.items():
            img = self.capture_region(hwnd, region_config, region_name)
            if img:
                text = self.ocr_image(img)
                results[region_name] = text.strip()
                self.logger.info(f"Region '{region_name}': {text.strip()}")
            else:
                results[region_name] = ""
        return results

    def extract_amount(self, amount_text: str) -> Optional[float]:
        """从金额文本中提取数值"""
        patterns = [
            r'¥?\s*(\d+\.\d{2})',
            r'(\d+\.\d+)\s*元',
            r'(\d+)',
        ]

        for pattern in patterns:
            match = re.search(pattern, amount_text)
            if match:
                return float(match.group(1))
        return None

    def parse_payment_info(self, regions_data: Dict[str, str], method: str) -> Optional[PaymentInfo]:
        """解析支付信息 - 只提取金额和玩家名，不验证商品"""
        try:
            # 1. 提取金额
            amount_text = regions_data.get('收款金额', '')
            amount = self.extract_amount(amount_text)
            if not amount:
                self.logger.warning(f"No amount extracted from: {amount_text}")
                return None

            # 2. 提取玩家名
            player_text = regions_data.get('付款方信息', '').strip()
            if not player_text:
                self.logger.warning("No player info found")
                return None

            return PaymentInfo(
                method=method,
                player=player_text,
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
                'amount': payment_info.amount,
                'regions_data': payment_info.regions_data
            }

            self.logger.info(f"Sending payment to server: {payment_info.player} - ¥{payment_info.amount}")

            response = requests.post(
                self.config['http_server']['url'],
                json=data,
                headers={'X-Secret-Key': self.config['http_server']['secret_key']},
                timeout=5
            )

            if response.status_code == 200:
                self.logger.info(f"Payment sent successfully: {payment_info.player} - ¥{payment_info.amount}")
                return True
            else:
                self.logger.error(f"Server error: {response.status_code} - {response.text}")
                return False
        except requests.exceptions.ConnectionError as e:
            self.logger.error(f"Connection failed: Cannot connect to {self.config['http_server']['url']}")
            return False
        except Exception as e:
            self.logger.error(f"Failed to send payment: {e}")
            return False

    def check_window(self, method: str, window_title: str, regions: Dict[str, Dict]):
        """检查指定窗口"""
        hwnd = self.find_window(window_title)
        if not hwnd:
            return

        regions_data = self.recognize_regions(hwnd, regions)

        # 检查订单序号（防重复）
        summary_text = regions_data.get('汇总', '')
        if summary_text:
            order_number = self.extract_order_number(summary_text)
            if order_number:
                last_number = self.last_order_number.get(method, 0)
                if order_number <= last_number:
                    self.logger.debug(f"Same order detected: 第{order_number}笔 (last: 第{last_number}笔)")
                    return
                self.logger.info(f"New order detected: 第{order_number}笔 (previous: 第{last_number}笔)")
            else:
                self.logger.warning(f"Cannot extract order number from: {summary_text}")

        payment_info = self.parse_payment_info(regions_data, method)
        if not payment_info:
            return

        if self.send_to_server(payment_info):
            # 更新最后处理的订单序号
            if summary_text and order_number:
                self.last_order_number[method] = order_number
                self.logger.info(f"Updated last order number: 第{order_number}笔")

    def run(self):
        """主循环"""
        self.logger.info("Monitor started")

        while True:
            try:
                if self.config['wechat']['enabled']:
                    self.check_window(
                        'wechat',
                        self.config['wechat']['window_title'],
                        self.config['wechat']['regions']
                    )

                if self.config['alipay']['enabled']:
                    self.check_window(
                        'alipay',
                        self.config['alipay']['window_title'],
                        self.config['alipay']['regions']
                    )

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

