import win32gui
import win32con
import win32ui
from PIL import Image
import numpy as np
import mss
import mss.tools
import time
import logging
import json
import re
import requests
from typing import Dict, Optional, Tuple
from dataclasses import dataclass

@dataclass
class PaymentInfo:
    method: str
    player: str
    product: str
    amount: float
    raw_data: Dict[str, str]

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

        self.logger.info("WindowMonitor initialized")

    def _init_ocr(self):
        """初始化OCR引擎"""
        try:
            import easyocr
            self.ocr_reader = easyocr.Reader(
                self.config['ocr']['languages'],
                gpu=False
            )
            self.logger.info(f"EasyOCR initialized with languages: {self.config['ocr']['languages']}")
        except Exception as e:
            self.logger.error(f"Failed to initialize OCR: {e}")
            raise

    def find_window(self, title: str) -> Optional[int]:
        """查找窗口句柄"""
        hwnd = win32gui.FindWindow(None, title)
        if hwnd == 0:
            return None
        return hwnd

    def capture_window(self, hwnd: int) -> Optional[Image.Image]:
        """截取窗口内容"""
        try:
            # 获取窗口位置
            rect = win32gui.GetWindowRect(hwnd)
            x, y, x1, y1 = rect
            width = x1 - x
            height = y1 - y

            # 使用mss截图(更快更稳定)
            with mss.mss() as sct:
                monitor = {
                    "top": y,
                    "left": x,
                    "width": width,
                    "height": height
                }
                screenshot = sct.grab(monitor)
                img = Image.frombytes('RGB', screenshot.size, screenshot.rgb)
                return img
        except Exception as e:
            self.logger.error(f"Failed to capture window: {e}")
            return None

    def ocr_image(self, img: Image.Image) -> str:
        """OCR识别图像文字"""
        try:
            # 转换PIL.Image为numpy array (EasyOCR要求的格式)
            img_array = np.array(img)

            results = self.ocr_reader.readtext(
                img_array,
                detail=1,
                paragraph=False
            )

            # 提取文字并按位置排序(从上到下,从左到右)
            texts = []
            for (bbox, text, confidence) in results:
                if confidence >= self.config['ocr']['confidence_threshold']:
                    # bbox格式: [[左上x,左上y], [右上x,右上y], [右下x,右下y], [左下x,左下y]]
                    y_pos = bbox[0][1]
                    texts.append((y_pos, text.strip()))

            # 按y坐标排序
            texts.sort(key=lambda x: x[0])
            full_text = '\n'.join([t[1] for t in texts])

            return full_text
        except Exception as e:
            self.logger.error(f"OCR error: {e}")
            return ""

    def parse_payment_info(self, text: str, method: str, fields: Dict[str, str]) -> Optional[PaymentInfo]:
        """解析支付信息"""
        try:
            lines = text.split('\n')
            data = {}

            # 查找键值对
            for key, value_type in fields.items():
                found = False
                for i, line in enumerate(lines):
                    if key in line:
                        # 找到键,下一行可能是值
                        if i + 1 < len(lines):
                            value = lines[i + 1].strip()
                            data[value_type] = value
                            found = True
                            break

                if not found:
                    # 尝试在同一行查找(键:值格式)
                    for line in lines:
                        if key in line:
                            parts = line.split(key)
                            if len(parts) > 1:
                                value = parts[1].strip().lstrip(':：').strip()
                                if value:
                                    data[value_type] = value
                                    found = True
                                    break

                if not found:
                    self.logger.debug(f"Field '{key}' not found in text")
                    return None

            # 验证必需字段
            if 'player' not in data or 'product' not in data or 'amount' not in data:
                self.logger.debug("Missing required fields")
                return None

            # 解析金额
            amount_str = data['amount']
            amount_match = re.search(r'(\d+\.?\d*)', amount_str)
            if not amount_match:
                self.logger.debug(f"Failed to parse amount: {amount_str}")
                return None
            amount = float(amount_match.group(1))

            payment = PaymentInfo(
                method=method,
                player=data['player'],
                product=data['product'],
                amount=amount,
                raw_data=data
            )

            return payment

        except Exception as e:
            self.logger.error(f"Failed to parse payment info: {e}")
            return None

    def send_to_server(self, payment: PaymentInfo) -> bool:
        """发送支付通知到服务器"""
        try:
            # 生成唯一ID(防重复)
            payment_id = f"{payment.method}_{payment.player}_{payment.amount}_{payment.product}"

            if payment_id in self.processed_payments:
                self.logger.debug(f"Payment already processed: {payment_id}")
                return False

            # 构建请求
            url = self.config['http_server']['url']
            headers = {
                'Content-Type': 'application/json',
                'X-Secret-Key': self.config['http_server']['secret_key']
            }
            data = {
                'method': payment.method,
                'player': payment.player,
                'product': payment.product,
                'amount': payment.amount,
                'raw_data': payment.raw_data
            }

            response = requests.post(url, json=data, headers=headers, timeout=5)

            if response.status_code == 200:
                self.processed_payments.add(payment_id)
                self.logger.info(f"Payment sent successfully: {payment.player} - {payment.product} - ¥{payment.amount}")
                return True
            else:
                self.logger.error(f"Server returned error: {response.status_code} - {response.text}")
                return False

        except Exception as e:
            self.logger.error(f"Failed to send payment to server: {e}")
            return False

    def monitor_method(self, method: str, config: dict):
        """监控单个支付方式"""
        window_title = config['window_title']
        fields = config['fields']
        interval = config['check_interval']

        self.logger.info(f"Starting monitor for {method}: {window_title}")

        last_text = ""
        while True:
            try:
                # 查找窗口
                hwnd = self.find_window(window_title)
                if hwnd is None:
                    time.sleep(interval)
                    continue

                # 截图
                img = self.capture_window(hwnd)
                if img is None:
                    time.sleep(interval)
                    continue

                # OCR识别
                text = self.ocr_image(img)
                if not text or text == last_text:
                    time.sleep(interval)
                    continue

                last_text = text
                self.logger.debug(f"OCR result:\n{text}")

                # 解析支付信息
                payment = self.parse_payment_info(text, method, fields)
                if payment is None:
                    time.sleep(interval)
                    continue

                # 发送到服务器
                self.send_to_server(payment)

                time.sleep(interval)

            except Exception as e:
                self.logger.error(f"Error in monitor loop: {e}")
                time.sleep(interval)

    def run(self):
        """运行所有启用的监控"""
        import threading

        threads = []

        # 微信监控
        if self.config['wechat']['enabled']:
            t = threading.Thread(
                target=self.monitor_method,
                args=('wechat', self.config['wechat']),
                daemon=True
            )
            t.start()
            threads.append(t)

        # 支付宝监控
        if self.config['alipay']['enabled']:
            t = threading.Thread(
                target=self.monitor_method,
                args=('alipay', self.config['alipay']),
                daemon=True
            )
            t.start()
            threads.append(t)

        # 等待所有线程
        for t in threads:
            t.join()

if __name__ == '__main__':
    monitor = WindowMonitor()
    monitor.run()
