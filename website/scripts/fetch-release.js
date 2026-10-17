import fs from "fs";
import path from "path";

const REPO = "mohammadmansour200/ligand";
const OUTPUT_PATH = path.join(process.cwd(), "src/data/release.json");

async function fetchLatestRelease() {
  try {
    console.log("Fetching latest release metadata from GitHub...");
    const res = await fetch(
      `https://api.github.com/repos/${REPO}/releases/latest`,
    );

    if (!res.ok) throw new Error(`GitHub API returned ${res.status}`);

    const data = await res.json();

    const releaseInfo = {
      version: data.tag_name,
      assets: data.assets.map((asset) => ({
        name: asset.name,
        downloadUrl: asset.browser_download_url,
        size: (asset.size / (1024 * 1024)).toFixed(2) + " MB",
      })),
    };

    fs.mkdirSync(path.dirname(OUTPUT_PATH), { recursive: true });
    fs.writeFileSync(OUTPUT_PATH, JSON.stringify(releaseInfo, null, 2));
    console.log(
      `Successfully generated release.json for ${releaseInfo.version}`,
    );
  } catch (error) {
    console.warn("Failed to fetch latest release. ", error.message);
  }
}

fetchLatestRelease();
