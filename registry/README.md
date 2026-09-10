# KuiklyUI third-party 登记说明

1. 先创建并公开 `https://github.com/Nobbyyinchen/KuiklyBanner`，推送本目录的仓库内容。
2. 确认示例链接可公开访问。
3. Fork `Tencent-TDS/KuiklyUI-third-party`。
4. 将 `KuiklyUI-Libraries-entry.json` 的对象追加到其 `KuiklyUI-Libraries.json` 数组末尾；也可以在当前官方基线直接应用 `KuiklyUI-Libraries.patch`。
5. 执行 JSON 校验并提交 PR。

PowerShell 校验命令：

```powershell
Get-Content -Raw KuiklyUI-Libraries.json | ConvertFrom-Json | Out-Null
```
