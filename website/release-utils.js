export const releaseConfig = {
    latestReleaseUrl: "https://api.github.com/repos/zhitongliu061115-cpu/BlogRecording/releases/latest",
    fallbackUrl: "https://github.com/zhitongliu061115-cpu/BlogRecording/releases/latest",
};

export function formatPublishedDate(publishedAt) {
    if (!publishedAt) {
        return "待发布";
    }

    const date = new Date(publishedAt);
    if (Number.isNaN(date.getTime())) {
        return "待发布";
    }

    return new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).format(date);
}

export function selectApkAsset(release) {
    if (!release || !Array.isArray(release.assets)) {
        return null;
    }

    return release.assets.find((asset) => {
        return typeof asset?.name === "string" && asset.name.toLowerCase().endsWith(".apk");
    }) || null;
}

export function resolveReleaseViewModel(payload, status = 200) {
    const fallbackModel = {
        version: "尚未发布",
        date: "待发布",
        status: "当前未检测到公开 APK，可先查看 Release 页面",
        href: releaseConfig.fallbackUrl,
    };

    if (status !== 200 || !payload || typeof payload !== "object") {
        return fallbackModel;
    }

    const apkAsset = selectApkAsset(payload);
    if (!apkAsset || !apkAsset.browser_download_url) {
        return fallbackModel;
    }

    return {
        version: payload.tag_name || payload.name || "最新版本",
        date: formatPublishedDate(payload.published_at),
        status: "已检测到最新公开 APK",
        href: apkAsset.browser_download_url,
    };
}
