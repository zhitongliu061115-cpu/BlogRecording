import assert from "node:assert/strict";
import { resolveReleaseViewModel } from "../website/release-utils.js";

const APK_URL = "https://example.com/app-release.apk";

const scenarios = [
    {
        name: "有 apk 资产时返回真实下载信息",
        payload: {
            tag_name: "v1.2.3",
            published_at: "2026-06-26T12:00:00Z",
            assets: [
                {
                    name: "app-release.apk",
                    browser_download_url: APK_URL,
                },
            ],
        },
        status: 200,
        expected: {
            version: "v1.2.3",
            date: "2026/06/26",
            status: "已检测到最新公开 APK",
            href: APK_URL,
        },
    },
    {
        name: "无 apk 资产时走 fallback",
        payload: {
            tag_name: "v1.2.3",
            published_at: "2026-06-26T12:00:00Z",
            assets: [
                {
                    name: "notes.txt",
                    browser_download_url: "https://example.com/notes.txt",
                },
            ],
        },
        status: 200,
        expected: {
            version: "尚未发布",
            date: "待发布",
            status: "当前未检测到公开 APK，可先查看 Release 页面",
            href: "https://github.com/zhitongliu061115-cpu/BlogRecording/releases/latest",
        },
    },
    {
        name: "404 时走 fallback",
        payload: null,
        status: 404,
        expected: {
            version: "尚未发布",
            date: "待发布",
            status: "当前未检测到公开 APK，可先查看 Release 页面",
            href: "https://github.com/zhitongliu061115-cpu/BlogRecording/releases/latest",
        },
    },
    {
        name: "混合资产时选中首个 apk",
        payload: {
            name: "First Release",
            published_at: "2026-06-25T08:30:00Z",
            assets: [
                {
                    name: "release-notes.md",
                    browser_download_url: "https://example.com/release-notes.md",
                },
                {
                    name: "podcast-recap-debug.apk",
                    browser_download_url: "https://example.com/podcast-recap-debug.apk",
                },
                {
                    name: "podcast-recap-arm64.apk",
                    browser_download_url: "https://example.com/podcast-recap-arm64.apk",
                },
            ],
        },
        status: 200,
        expected: {
            version: "First Release",
            date: "2026/06/25",
            status: "已检测到最新公开 APK",
            href: "https://example.com/podcast-recap-debug.apk",
        },
    },
];

for (const scenario of scenarios) {
    const actual = resolveReleaseViewModel(scenario.payload, scenario.status);
    assert.deepEqual(actual, scenario.expected, scenario.name);
}

console.log("website release logic tests passed");
