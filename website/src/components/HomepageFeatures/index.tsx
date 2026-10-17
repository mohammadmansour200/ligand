import type { ReactNode } from "react";
import clsx from "clsx";

import Heading from "@theme/Heading";

import styles from "./styles.module.css";

type FeatureItem = {
  title: string;
  imageSrc: string;
  imageAlt: string;
  imageWidth: number;
  imageHeight: number;
  description: ReactNode;
};

const FEATURES: FeatureItem[] = [
  {
    title: "Multiple Visualization Modes",
    imageSrc: "img/Ligand_3D_Modes.webp",
    imageAlt:
      "Ethanol shown as ball-and-stick, space-filling, and electrostatic potential map models",
    imageWidth: 1002,
    imageHeight: 294,
    description:
      "View drawn structures in ball-and-stick to study geometry, space-filling to examine steric bulk and electrostatic potential map to visualize polarity and charge distribution.",
  },
  {
    title: "Comprehensive Structure Analysis",
    imageSrc: "img/Ligand_Analysis.webp",
    imageAlt:
      "A 5-fluorouracil structure at the centre, with nomenclature, formula, weight, LogP, hydrogen bonding and rotatable bonds radiating evenly from it",
    imageWidth: 1000,
    imageHeight: 694,
    description:
      "Ligand analyzes drawn structure for nomenclature, molecular formula, molecular weight, LogP, hydrogen bonding, and rotatable bonds.",
  },
  {
    title: "Lightweight & Portable",
    imageSrc: "img/Ligand_Portable.webp",
    imageAlt: "A USB drive with Ligand's icon",
    imageWidth: 690,
    imageHeight: 271,
    description:
      "Available for Android, Linux, and  Windows. Lightweight in size and portable to the extent of working directly from a USB drive for easy educational use.",
  },
  {
    title: "Powered by RDKit, OPSIN, and PubChem",
    imageSrc: "img/Ligand_Powered_By.webp",
    imageAlt: "RDKit, OPSIN, and PubChem logo",
    imageWidth: 1000,
    imageHeight: 413,
    description:
      "Ligand relies on well-established, open-source scientific tools and databases.",
  },
];

function Feature({
  title,
  imageSrc,
  imageAlt,
  imageWidth,
  imageHeight,
  description,
}: FeatureItem) {
  return (
    <div className={clsx("col col--4", styles.col)}>
      <article className={styles.card}>
        <div className={clsx(styles.stage)}>
          <img
            className={clsx(styles.image)}
            src={imageSrc}
            alt={imageAlt}
            width={imageWidth}
            height={imageHeight}
            loading="lazy"
            decoding="async"
          />
        </div>
        <div className={styles.body}>
          <Heading as="h3" className={styles.title}>
            {title}
          </Heading>
          <p className={styles.description}>{description}</p>
        </div>
      </article>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <Heading as="h2" className="text--center">
          Features
        </Heading>
        <div className={clsx("row", styles.row)}>
          {FEATURES.map((feature) => (
            <Feature key={feature.title} {...feature} />
          ))}
        </div>
      </div>
    </section>
  );
}
