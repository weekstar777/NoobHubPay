import requests
import json
import sys

def test_payment(secret_key="change-this-to-random-secret-key"):
    """测试支付接口"""

    url = "http://127.0.0.1:8787/payment"
    headers = {
        "Content-Type": "application/json",
        "X-Secret-Key": secret_key
    }

    # 测试数据
    test_data = {
        "method": "test",
        "player": "TestPlayer",
        "product": "64个钻石块",
        "amount": 68.00,
        "raw_data": {
            "收款金额": "68.00",
            "收款项": "64个钻石块",
            "付款方信息": "TestPlayer"
        }
    }

    print("=" * 50)
    print("NoobHubPay 接口测试")
    print("=" * 50)
    print(f"\n[1] 测试连接: {url}")

    try:
        # 测试健康检查
        health_response = requests.get("http://127.0.0.1:8787/health", timeout=3)
        if health_response.status_code == 200:
            print("✓ 服务器在线")
        else:
            print(f"✗ 健康检查失败: {health_response.status_code}")
            return False
    except Exception as e:
        print(f"✗ 无法连接到服务器: {e}")
        print("\n请确认:")
        print("  1. Minecraft服务器正在运行")
        print("  2. NoobHubPay插件已加载")
        print("  3. HTTP服务器已启动(检查config.yml中的http-server.enabled)")
        return False

    print(f"\n[2] 发送测试支付")
    print(f"   玩家: {test_data['player']}")
    print(f"   商品: {test_data['product']}")
    print(f"   金额: ¥{test_data['amount']}")

    try:
        response = requests.post(url, json=test_data, headers=headers, timeout=5)

        print(f"\n[3] 服务器响应")
        print(f"   状态码: {response.status_code}")
        print(f"   响应: {response.text}")

        if response.status_code == 200:
            result = response.json()
            if result.get('success'):
                print("\n✓ 支付测试成功!")
                print("\n检查游戏服务器:")
                print(f"  1. 玩家 {test_data['player']} 是否在线?")
                print(f"  2. 玩家是否收到了商品?")
                print(f"  3. 查看服务器日志确认发货命令执行")
                return True
            else:
                print(f"\n✗ 支付失败: {result.get('message')}")
                return False
        elif response.status_code == 403:
            print("\n✗ 密钥验证失败!")
            print("请检查:")
            print("  1. monitor/config.json 中的 secret_key")
            print("  2. plugins/NoobHubPay/config.yml 中的 http-server.secret-key")
            print("  3. 两者必须完全一致")
            return False
        elif response.status_code == 400:
            print("\n✗ 请求数据错误")
            print("可能原因:")
            print("  1. 玩家不在线")
            print("  2. 商品不存在")
            print("  3. 金额不匹配")
            return False
        else:
            print(f"\n✗ 未知错误: {response.status_code}")
            return False

    except Exception as e:
        print(f"\n✗ 请求失败: {e}")
        return False

if __name__ == '__main__':
    print("NoobHubPay 测试工具\n")

    # 从配置文件读取密钥
    try:
        with open('config.json', 'r', encoding='utf-8') as f:
            config = json.load(f)
            secret_key = config['http_server']['secret_key']
    except:
        print("警告: 无法读取config.json,使用默认密钥")
        secret_key = "change-this-to-random-secret-key"

    success = test_payment(secret_key)

    if success:
        sys.exit(0)
    else:
        print("\n" + "=" * 50)
        print("测试失败 - 排查建议:")
        print("=" * 50)
        print("1. 使用 /payadmin status 检查插件状态")
        print("2. 使用 /payadmin list 查看商品配置")
        print("3. 查看日志: logs/latest.log")
        print("4. 确保测试玩家在线: /list")
        sys.exit(1)
