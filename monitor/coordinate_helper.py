"""
坐标定位辅助工具
用于帮助确定窗口内特定区域的坐标
"""
import win32gui
import win32con
from PIL import Image, ImageDraw, ImageFont
import mss
import sys

def find_window(title):
    """查找窗口"""
    hwnd = win32gui.FindWindow(None, title)
    if hwnd == 0:
        print(f"未找到窗口: {title}")
        return None
    return hwnd

def capture_window_with_grid(hwnd):
    """截取窗口并绘制网格"""
    # 获取客户区坐标
    client_rect = win32gui.GetClientRect(hwnd)
    client_left_top = win32gui.ClientToScreen(hwnd, (client_rect[0], client_rect[1]))
    client_right_bottom = win32gui.ClientToScreen(hwnd, (client_rect[2], client_rect[3]))

    width = client_right_bottom[0] - client_left_top[0]
    height = client_right_bottom[1] - client_left_top[1]

    print(f"窗口客户区大小: {width} x {height}")

    # 截图
    with mss.mss() as sct:
        monitor = {
            "top": client_left_top[1],
            "left": client_left_top[0],
            "width": width,
            "height": height
        }
        screenshot = sct.grab(monitor)
        img = Image.frombytes('RGB', screenshot.size, screenshot.rgb)

    # 绘制网格
    draw = ImageDraw.Draw(img)

    # 绘制垂直线（每50像素）
    for x in range(0, width, 50):
        draw.line([(x, 0), (x, height)], fill='red', width=1)
        if x % 100 == 0:
            draw.text((x + 2, 2), str(x), fill='red')

    # 绘制水平线（每50像素）
    for y in range(0, height, 50):
        draw.line([(0, y), (width, y)], fill='red', width=1)
        if y % 100 == 0:
            draw.text((2, y + 2), str(y), fill='red')

    return img

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python coordinate_helper.py <窗口标题>")
        print("示例: python coordinate_helper.py 服务通知")
        sys.exit(1)

    window_title = sys.argv[1]
    hwnd = find_window(window_title)

    if hwnd:
        img = capture_window_with_grid(hwnd)
        output_file = "window_grid.png"
        img.save(output_file)
        print(f"已保存网格截图到: {output_file}")
        print("\n使用说明:")
        print("1. 打开 window_grid.png")
        print("2. 找到要识别的区域")
        print("3. 记录区域的 x, y 坐标（左上角）")
        print("4. 记录区域的 width, height（宽度和高度）")
        print("5. 将坐标填入 config.json 的 regions 配置中")
