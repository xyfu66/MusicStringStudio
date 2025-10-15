#!/bin/bash

# MusicXML 转 MP3 自动化脚本
# 使用 MuseScore 命令行工具批量转换

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# 默认参数
MUSESCORE_PATH="${MUSESCORE_PATH:-/usr/bin/musescore4}"
BITRATE="${BITRATE:-192}"

# 检查是否指定了自定义路径
if [ "$1" != "" ]; then
    MUSESCORE_PATH="$1"
fi

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  MusicXML 转 MP3 自动化工具${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""

# 检查 MuseScore 是否存在
if [ ! -f "$MUSESCORE_PATH" ]; then
    # 尝试常见路径
    COMMON_PATHS=(
        "/usr/bin/musescore4"
        "/usr/bin/musescore"
        "/usr/local/bin/musescore4"
        "/usr/local/bin/musescore"
        "/Applications/MuseScore 4.app/Contents/MacOS/mscore"
        "$HOME/Applications/MuseScore 4.app/Contents/MacOS/mscore"
    )
    
    FOUND=false
    for path in "${COMMON_PATHS[@]}"; do
        if [ -f "$path" ]; then
            MUSESCORE_PATH="$path"
            FOUND=true
            break
        fi
    done
    
    if [ "$FOUND" = false ]; then
        echo -e "${RED}错误: 找不到 MuseScore!${NC}"
        echo "尝试过的路径: $MUSESCORE_PATH"
        echo ""
        echo "请安装 MuseScore 4 或指定正确的路径:"
        echo "  ./generate_audio.sh /path/to/musescore4"
        echo ""
        echo "下载 MuseScore: https://musescore.org/"
        exit 1
    fi
fi

echo -e "${CYAN}MuseScore 路径: $MUSESCORE_PATH${NC}"
echo ""

# 定义目录
XML_DIR="app/src/main/res/raw"
AUDIO_DIR="app/src/main/assets/audio"

# 检查 XML 目录是否存在
if [ ! -d "$XML_DIR" ]; then
    echo -e "${RED}错误: 找不到 MusicXML 目录!${NC}"
    echo "预期路径: $XML_DIR"
    exit 1
fi

# 创建输出目录
if [ ! -d "$AUDIO_DIR" ]; then
    echo -e "${YELLOW}创建输出目录: $AUDIO_DIR${NC}"
    mkdir -p "$AUDIO_DIR"
fi

# 查找所有 .musicxml 文件
XML_FILES=("$XML_DIR"/*.musicxml)

# 检查是否找到文件
if [ ! -e "${XML_FILES[0]}" ]; then
    echo -e "${YELLOW}警告: 没有找到 .musicxml 文件${NC}"
    echo "请确保 $XML_DIR 目录下有 MusicXML 文件"
    exit 0
fi

# 计算文件数量
FILE_COUNT=${#XML_FILES[@]}
echo -e "${CYAN}找到 $FILE_COUNT 个 MusicXML 文件${NC}"
echo ""

# 转换计数器
SUCCESS_COUNT=0
FAIL_COUNT=0

# 遍历所有文件
for xml_file in "${XML_FILES[@]}"; do
    filename=$(basename "$xml_file" .musicxml)
    output_file="$AUDIO_DIR/$filename.mp3"
    
    echo -e "${WHITE}转换: $(basename "$xml_file")${NC}"
    echo "  -> $filename.mp3"
    
    # 使用 MuseScore 命令行转换
    # -o: 输出文件
    # -r: 比特率 (部分版本支持)
    if "$MUSESCORE_PATH" "$xml_file" -o "$output_file" 2>/dev/null; then
        if [ -f "$output_file" ]; then
            file_size=$(du -h "$output_file" | cut -f1)
            echo -e "${GREEN}  ✓ 成功 ($file_size)${NC}"
            ((SUCCESS_COUNT++))
        else
            echo -e "${RED}  ✗ 失败: 输出文件未创建${NC}"
            ((FAIL_COUNT++))
        fi
    else
        echo -e "${RED}  ✗ 失败: MuseScore 转换错误${NC}"
        ((FAIL_COUNT++))
    fi
    
    echo ""
done

# 显示总结
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  转换完成${NC}"
echo -e "${GREEN}=========================================${NC}"
echo "成功: $SUCCESS_COUNT"
echo "失败: $FAIL_COUNT"
echo "总计: $FILE_COUNT"
echo ""
echo "输出目录: $AUDIO_DIR"
echo ""

if [ $SUCCESS_COUNT -gt 0 ]; then
    echo -e "${CYAN}音频文件已生成！${NC}"
    echo "可以在 Android 项目中使用这些文件了。"
fi

if [ $FAIL_COUNT -gt 0 ]; then
    echo -e "${YELLOW}注意: 某些文件转换失败，请检查上面的错误信息。${NC}"
fi
