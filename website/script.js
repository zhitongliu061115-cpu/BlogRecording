import { releaseConfig, resolveReleaseViewModel } from "./release-utils.js";

function applyReleaseViewModel(model) {
    const versionNode = document.querySelector("[data-release-version]");
    const dateNode = document.querySelector("[data-release-date]");
    const statusNode = document.querySelector("[data-release-status]");
    const linkNodes = document.querySelectorAll("[data-release-link]");

    if (versionNode) {
        versionNode.textContent = model.version;
    }
    if (dateNode) {
        dateNode.textContent = model.date;
    }
    if (statusNode) {
        statusNode.textContent = model.status;
    }
    linkNodes.forEach((node) => {
        node.setAttribute("href", model.href);
    });
}

async function hydrateLatestRelease() {
    applyReleaseViewModel({
        version: "正在检测",
        date: "正在检测",
        status: "正在检测最新 Release",
        href: releaseConfig.fallbackUrl,
    });

    try {
        const response = await fetch(releaseConfig.latestReleaseUrl, {
            headers: {
                Accept: "application/vnd.github+json",
            },
        });
        const payload = response.ok ? await response.json() : null;
        applyReleaseViewModel(resolveReleaseViewModel(payload, response.status));
    } catch (error) {
        applyReleaseViewModel(resolveReleaseViewModel(null, 500));
        console.warn("Failed to fetch latest release.", error);
    }
}

function setupRevealAnimations() {
    const nodes = document.querySelectorAll(".reveal");
    if (!("IntersectionObserver" in window)) {
        nodes.forEach((node) => node.classList.add("is-visible"));
        return;
    }

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) {
                return;
            }
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
        });
    }, {
        threshold: 0.15,
        rootMargin: "0px 0px -48px 0px",
    });

    nodes.forEach((node) => observer.observe(node));
}

setupRevealAnimations();
hydrateLatestRelease();
