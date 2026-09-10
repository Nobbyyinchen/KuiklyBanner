# Contributing

感谢参与 KuiklyBanner 的改进。

1. 新建分支并保持改动聚焦。
2. 公共 API 变更需同步更新 `README.md` 和示例。
3. 循环索引或影子节点算法变更需补充 `commonTest`。
4. 提交前运行：

   ```shell
   ./gradlew :kuikly-banner:jsNodeTest \
     :kuikly-banner:compileReleaseKotlinAndroid \
     :sample:compileKotlinJs \
     :sample:compileReleaseKotlinAndroid
   ```

5. PR 中说明影响的平台、是否影响动态化，以及手动验证过的交互场景。
