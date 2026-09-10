const path = require("node:path");
const { pathToFileURL } = require("node:url");

const playwrightModule = process.env.PLAYWRIGHT_MODULE || "playwright";
const { chromium } = require(playwrightModule);

async function main() {
  const executablePath = process.env.CHROME_PATH;
  const browser = await chromium.launch({
    headless: true,
    executablePath: executablePath || undefined,
  });
  const page = await browser.newPage({ acceptDownloads: true });
  const generatorUrl = pathToFileURL(path.join(__dirname, "generate-demo.html"));

  for (const demo of ["peek", "double", "vertical"]) {
    const downloadPromise = page.waitForEvent("download", { timeout: 20_000 });
    await page.goto(`${generatorUrl.href}?demo=${demo}`);
    await page.waitForTimeout(120);
    await page.locator("canvas").screenshot({
      path: path.join(__dirname, `${demo}-banner.png`),
    });
    const download = await downloadPromise;
    await download.saveAs(path.join(__dirname, `${demo}-banner.webm`));
  }

  await browser.close();
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
