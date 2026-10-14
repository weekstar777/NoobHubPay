"""
新的支付信息解析器 - 基于内容匹配
"""
import re
from typing import Optional, Dict, List
from dataclasses import dataclass

@dataclass
class PaymentInfo:
    method: str
    player: str
    product: str
    amount: float
    raw_data: Dict

class ContentMatchParser:
    """基于内容匹配的解析器"""

    def __init__(self, logger, products: List[Dict]):
        self.logger = logger
        self.products = products  # [{'name': '商品名', 'price': 价格}, ...]

    def parse(self, text: str, method: str) -> Optional[PaymentInfo]:
        """
        解析支付信息 - 新逻辑

        步骤:
        1. 提取金额
        2. 查找匹配的商品（商品名在文字中 + 金额匹配）
        3. 提取玩家名
        """
        self.logger.info("=" * 50)
        self.logger.info(f"OCR识别原始文本 ({method}):")
        self.logger.info(text)
        self.logger.info("=" * 50)

        # 1. 提取金额
        amount = self._extract_amount(text)
        if not amount:
            self.logger.warning("✗ 未找到金额")
            return None
        self.logger.info(f"✓ 提取金额: ¥{amount}")

        # 2. 查找商品
        product = self._find_product(text, amount)
        if not product:
            self.logger.warning(f"✗ 未找到匹配的商品 (金额: ¥{amount})")
            return None
        self.logger.info(f"✓ 匹配商品: {product['name']} (¥{product['price']})")

        # 3. 提取玩家名
        player = self._extract_player(text, product['name'], amount)
        if not player:
            self.logger.warning("✗ 未找到玩家名")
            return None
        self.logger.info(f"✓ 提取玩家名: {player}")

        payment = PaymentInfo(
            method=method,
            player=player,
            product=product['name'],
            amount=amount,
            raw_data={'ocr_text': text}
        )

        self.logger.info(f"✓ 解析成功: {payment.player} - {payment.product} - ¥{payment.amount}")
        return payment

    def _extract_amount(self, text: str) -> Optional[float]:
        """提取金额"""
        # 查找数字模式
        patterns = [
            r'¥?\s*(\d+\.\d{2})',  # ¥68.00 或 68.00
            r'(\d+\.\d+)\s*元',     # 68.00元
            r'[^\d](\d{2,4}\.\d{1,2})[^\d]',  # 被其他字符包围的数字
        ]

        for pattern in patterns:
            matches = re.findall(pattern, text)
            for match in matches:
                try:
                    val = float(match)
                    if 0.01 <= val <= 10000:  # 合理范围
                        return val
                except:
                    continue

        return None

    def _find_product(self, text: str, amount: float) -> Optional[Dict]:
        """
        查找商品 - 商品名在文字中 + 金额匹配
        """
        # 归一化文本（去除空格和特殊符号）
        text_normalized = re.sub(r'[^\w一-龥]', '', text)

        for product in self.products:
            # 检查金额是否匹配
            if abs(product['price'] - amount) < 0.01:
                # 检查商品名是否在文字中
                product_normalized = re.sub(r'[^\w一-龥]', '', product['name'])

                if product_normalized in text_normalized or product['name'] in text:
                    return product

        return None

    def _extract_player(self, text: str, product_name: str, amount: float) -> Optional[str]:
        """
        提取玩家名

        策略:
        1. 去除商品名、金额、常见关键字
        2. 查找冒号后的值（如 "玩家:dream"）
        3. 提取剩余的有效字符串
        """
        # 先尝试查找冒号后的值
        colon_patterns = [
            r'[:：]\s*([a-zA-Z0-9_一-龥]{2,16})',  # :后面的玩家名
            r'玩家[:：]?\s*([a-zA-Z0-9_一-龥]{2,16})',
            r'付款方[:：]?\s*([a-zA-Z0-9_一-龥]{2,16})',
        ]

        for pattern in colon_patterns:
            match = re.search(pattern, text)
            if match:
                player = match.group(1).strip()
                # 过滤掉不像玩家名的
                if player and not any(kw in player for kw in ['必填', '信息', '备注', '金额']):
                    return player

        # 如果没找到，使用清理策略
        player_text = text

        # 去除商品名
        player_text = player_text.replace(product_name, ' ')

        # 去除金额
        player_text = re.sub(r'¥?\s*\d+\.?\d*\s*元?', ' ', player_text)

        # 去除常见关键字
        keywords = [
            '收款金额', '收款项', '付款方', '付款方信息', '备注', '转账',
            '必填', '(必填)', 'X1', 'x1', '月', '年', 'VIP', 'vip',
            '\n', '\r', '\t'
        ]
        for kw in keywords:
            player_text = player_text.replace(kw, ' ')

        # 清理符号
        player_text = re.sub(r'[^\w一-龥\s]', ' ', player_text)

        # 分割并查找有效部分
        parts = [p.strip() for p in player_text.split() if p.strip()]

        # 找最可能是玩家名的（字母数字下划线中文，长度2-16）
        for part in parts:
            if re.match(r'^[a-zA-Z0-9_一-龥]{2,16}$', part):
                # 排除纯数字
                if not part.isdigit():
                    return part

        # 如果还没找到，返回第一个非空部分
        if parts:
            return parts[0]

        return None
