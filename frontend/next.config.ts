import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Exporta HTML estático (pasta out/): o front não precisa de servidor Node, só de hospedagem de arquivos.
  output: "export",
  reactCompiler: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
