import type { ReactNode } from "react";

import Link from "@docusaurus/Link";

import Heading from "@theme/Heading";

import releaseData from "@site/src/data/release.json";
import styles from "./styles.module.css";

interface AssetButton {
  label: string;
  extension: string;
}

interface Platform {
  platform: string;
  Svg: React.ComponentType<React.ComponentProps<"svg">>;
  buttons: AssetButton[];
}

const PLATFORMS: Platform[] = [
  {
    platform: "Android",
    Svg: require("@site/static/img/Android_Logo.svg").default,
    buttons: [{ label: "Android", extension: ".apk" }],
  },
  {
    platform: "Linux",
    Svg: require("@site/static/img/Linux_Logo.svg").default,
    buttons: [
      { label: "Debian/Ubuntu", extension: ".deb" },
      { label: "Red Hat/Fedora", extension: ".rpm" },
    ],
  },
  {
    platform: "Windows",
    Svg: require("@site/static/img/Windows_Logo.svg").default,
    buttons: [
      { label: "Installer", extension: ".msi" },
      { label: "Portable", extension: ".zip" },
    ],
  },
];

function findAsset(extension: string) {
  return releaseData.assets.find((a) => a.name.endsWith(extension));
}

function DownloadIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <polyline points="7 10 12 15 17 10" />
      <line x1="12" y1="15" x2="12" y2="3" />
    </svg>
  );
}

function PlatformCard({ platform, Svg, buttons }: Platform): ReactNode {
  return (
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        <Svg role="img" />
        <span className={styles.platformName}>{platform}</span>
      </div>
      <div className={styles.cardFooter}>
        {buttons.map((btn) => {
          const asset = findAsset(btn.extension);
          return (
            <Link
              key={btn.label}
              className={styles.downloadBtn}
              href={asset?.downloadUrl}
            >
              {btn.label} ({asset?.size}) <DownloadIcon />
            </Link>
          );
        })}
      </div>
    </div>
  );
}

export default function HomepageDownload(): ReactNode {
  return (
    <section id="download" className={styles.downloadSection}>
      <div className="container">
        <Heading as="h2" className="text--center">
          Download Ligand {releaseData.version}
        </Heading>
        <div className={styles.grid}>
          {PLATFORMS.map((info) => (
            <PlatformCard key={info.platform} {...info} />
          ))}
        </div>
      </div>
    </section>
  );
}
