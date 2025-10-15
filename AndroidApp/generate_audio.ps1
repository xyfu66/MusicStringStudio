# MusicXML 转 MP3 自动化脚本
# 使用 MuseScore 命令行工具批量转换

param(
    [string]$MuseScorePath = "C:\Program Files\MuseScore 4\bin\MuseScore4.exe",
    [int]$BitRate = 192
)

# 颜色输出函数
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Green "========================================="
Write-ColorOutput Green "  MusicXML 转 MP3 自动化工具"
Write-ColorOutput Green "========================================="
Write-Output ""

# 检查 MuseScore 是否存在
if (-Not (Test-Path $MuseScorePath)) {
    Write-ColorOutput Red "错误: 找不到 MuseScore!"
    Write-Output "默认路径: $MuseScorePath"
    Write-Output ""
    Write-Output "请安装 MuseScore 4 或指定正确的路径:"
    Write-Output "  .\generate_audio.ps1 -MuseScorePath 'C:\your\path\MuseScore4.exe'"
    Write-Output ""
    Write-Output "下载 MuseScore: https://musescore.org/"
    exit 1
}

Write-ColorOutput Cyan "MuseScore 路径: $MuseScorePath"
Write-Output ""

# 定义目录
$xmlDir = "app\src\main\res\raw"
$audioDir = "app\src\main\assets\audio"

# 检查 XML 目录是否存在
if (-Not (Test-Path $xmlDir)) {
    Write-ColorOutput Red "错误: 找不到 MusicXML 目录!"
    Write-Output "预期路径: $xmlDir"
    exit 1
}

# 创建输出目录
if (-Not (Test-Path $audioDir)) {
    Write-ColorOutput Yellow "创建输出目录: $audioDir"
    New-Item -ItemType Directory -Force -Path $audioDir | Out-Null
}

# 获取所有 .musicxml 文件
$xmlFiles = Get-ChildItem $xmlDir -Filter *.musicxml

if ($xmlFiles.Count -eq 0) {
    Write-ColorOutput Yellow "警告: 没有找到 .musicxml 文件"
    Write-Output "请确保 $xmlDir 目录下有 MusicXML 文件"
    exit 0
}

Write-ColorOutput Cyan "找到 $($xmlFiles.Count) 个 MusicXML 文件"
Write-Output ""

# 转换计数器
$successCount = 0
$failCount = 0

# 遍历所有文件
foreach ($xmlFile in $xmlFiles) {
    $inputFile = $xmlFile.FullName
    $outputFile = Join-Path $audioDir ($xmlFile.BaseName + ".mp3")
    
    Write-ColorOutput White "转换: $($xmlFile.Name)"
    Write-Output "  -> $($xmlFile.BaseName).mp3"
    
    try {
        # 使用 MuseScore 命令行转换
        # -o: 输出文件
        # -r: 比特率
        $process = Start-Process -FilePath $MuseScorePath `
            -ArgumentList "`"$inputFile`"", "-o", "`"$outputFile`"", "-r", "$BitRate" `
            -NoNewWindow -Wait -PassThru
        
        if ($process.ExitCode -eq 0) {
            if (Test-Path $outputFile) {
                $fileSize = (Get-Item $outputFile).Length / 1KB
                Write-ColorOutput Green "  ✓ 成功 ($([math]::Round($fileSize, 2)) KB)"
                $successCount++
            } else {
                Write-ColorOutput Red "  ✗ 失败: 输出文件未创建"
                $failCount++
            }
        } else {
            Write-ColorOutput Red "  ✗ 失败: MuseScore 返回错误代码 $($process.ExitCode)"
            $failCount++
        }
    }
    catch {
        Write-ColorOutput Red "  ✗ 失败: $_"
        $failCount++
    }
    
    Write-Output ""
}

# 显示总结
Write-Output ""
Write-ColorOutput Green "========================================="
Write-ColorOutput Green "  转换完成"
Write-ColorOutput Green "========================================="
Write-Output "成功: $successCount"
Write-Output "失败: $failCount"
Write-Output "总计: $($xmlFiles.Count)"
Write-Output ""
Write-Output "输出目录: $audioDir"
Write-Output ""

if ($successCount -gt 0) {
    Write-ColorOutput Cyan "音频文件已生成！"
    Write-Output "可以在 Android 项目中使用这些文件了。"
}

if ($failCount -gt 0) {
    Write-ColorOutput Yellow "注意: 某些文件转换失败，请检查上面的错误信息。"
}
