import { themes as prismThemes } from "prism-react-renderer";
import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const config: Config = {
  title: "Ligand",
  tagline:
    "Open-source application for sketching, 3D visualizing, and analyzing organic chemistry structure.",
  favicon: "img/favicon.ico",

  future: {
    v4: true,
  },

  url: "https://mohammadmansour200.github.io",

  baseUrl: "/ligand/",

  organizationName: "mohammadmansour200",
  projectName: "ligand",

  onBrokenLinks: "throw",

  i18n: {
    defaultLocale: "en",
    locales: ["en", "ar"],
    localeConfigs: {
      ar: {
        direction: "rtl",
      },
    },
  },

  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          editUrl:
            "https://github.com/mohammadmansour200/ligand/tree/main/website",
        },
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: "img/Ligand_Social_Card.jpg",
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: "Ligand",
      logo: {
        alt: "Ligand's logo featuring 5-Fluorouracil",
        src: "img/logo.svg",
      },
      items: [
        {
          type: "doc",
          docId: "contributing/index",
          position: "left",
          label: "Contributing",
        },
        {
          href: "https://github.com/mohammadmansour200/ligand",
          position: "right",
          className: "header-github-link",
          "aria-label": "GitHub repository",
        },
        // { type: "localeDropdown", position: "right" },
      ],
    },
    footer: {
      links: [
        {
          title: "Docs",
          items: [
            {
              label: "Contributing",
              to: "/docs/contributing",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "GitHub Issues",
              href: "https://github.com/mohammadmansour200/ligand/issues",
            },
            {
              label: "GitHub Pull Requests",
              href: "https://github.com/mohammadmansour200/ligand/pulls",
            },
            {
              label: "GitHub Discussions",
              href: "https://github.com/mohammadmansour200/ligand/discussions",
            },
          ],
        },
        {
          title: "Contact",
          items: [
            {
              label: "LinkedIn",
              href: "https://www.linkedin.com/in/mohammadtabdulkader/",
            },
            {
              label: "Email",
              href: "mailto:mohammadamansour03@gmail.com",
            },
            {
              label: "Donate",
              href: "https://ko-fi.com/ligand",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Mohammad Abd Al-Tawab. Built with Docusaurus.<br /><small>RDKit, OPSIN, and PubChem names and logos belong to their respective owners and are used for identification only. Their use does not imply endorsement of or affiliation with Ligand.</small>`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
