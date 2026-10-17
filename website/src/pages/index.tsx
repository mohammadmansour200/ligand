import type { ReactNode } from "react";
import { useMemo } from "react";
import clsx from "clsx";

import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import useIsBrowser from "@docusaurus/useIsBrowser";

import Layout from "@theme/Layout";
import Heading from "@theme/Heading";

import HomepageFeatures from "@site/src/components/HomepageFeatures";
import HomepageDownload from "@site/src/components/HomepageDownload";
import releaseData from "@site/src/data/release.json";
import styles from "./index.module.css";

const GITHUB_RELEASES_URL =
  "https://github.com/mohammadmansour200/ligand/releases";

type Platform = "android" | "windows" | "linux" | "unknown";

function detectPlatform(): Platform {
  if (typeof navigator === "undefined") return "unknown";

  const ua = navigator.userAgent.toLowerCase();

  if (/android/.test(ua)) return "android";
  if (/win/.test(ua)) return "windows";
  if (/linux/.test(ua)) return "linux";

  return "unknown";
}

interface PlatformDownload {
  label: string;
  url: string;
}

const FALLBACK_DOWNLOAD = [{ label: "Download", url: GITHUB_RELEASES_URL }];
function getDownloadForPlatform(platform: Platform): PlatformDownload[] {
  if (platform === "unknown") return FALLBACK_DOWNLOAD;

  if (platform === "android")
    return [
      {
        label: "Download for Android",
        url: releaseData.assets.find((a) => a.name.endsWith(".apk"))
          ?.downloadUrl!,
      },
    ];

  if (platform === "windows")
    return [
      {
        label: "Download for Windows",
        url: releaseData.assets.find((a) => a.name.endsWith(".msi"))
          ?.downloadUrl!,
      },
    ];

  if (platform === "linux")
    return [
      {
        label: "Download for Linux (.deb)",
        url: releaseData.assets.find((a) => a.name.endsWith(".deb"))
          ?.downloadUrl!,
      },
      {
        label: "Download for Linux (.rpm)",
        url: releaseData.assets.find((a) => a.name.endsWith(".rpm"))
          ?.downloadUrl!,
      },
    ];

  return FALLBACK_DOWNLOAD;
}

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext();
  const isBrowser = useIsBrowser();

  const downloads = useMemo(() => {
    const platform = isBrowser ? detectPlatform() : "unknown";
    return getDownloadForPlatform(platform);
  }, [isBrowser]);

  return (
    <header className={styles.heroBanner}>
      <div className={clsx("container", styles.container)}>
        <div>
          <Heading as="h1" className="hero__title">
            {siteConfig.title}
          </Heading>
          <p className="hero__subtitle">{siteConfig.tagline}</p>
          <div className={styles.download}>
            <div className={styles.buttons}>
              {downloads.map((download) => (
                <Link
                  key={download.url}
                  className="button button--primary button--lg"
                  href={download.url}
                >
                  {download.label}
                </Link>
              ))}
            </div>
            <Link to="/#download">All Platforms</Link>
          </div>
        </div>
        <img
          src="img/Ligand_Header.webp"
          alt="Ligand's Interface"
          width="1200"
          height="704"
          loading="eager"
          fetchPriority="high"
        />
      </div>
    </header>
  );
}

export default function Home(): ReactNode {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>
      <HomepageHeader />
      <main>
        <HomepageFeatures />
        <HomepageDownload />
      </main>
    </Layout>
  );
}
