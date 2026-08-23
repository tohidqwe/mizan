import { p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { d as Check, f as Bookmark, o as RotateCcw } from "../_libs/lucide-react.mjs";
import { E as toFaDigits, T as cn, a as Button, d as useMizan, h as getArticle, m as articleKey, y as LAW_BY_ID } from "./router-D3fRPlX_.mjs";
import { t as Badge } from "./badge-Bo9iRCUf.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/article-block-BV72o_Vw.js
var import_jsx_runtime = require_jsx_runtime();
function ArticleBlock({ article, compact = false }) {
	const key = articleKey(article.lawId, article.n);
	const law = LAW_BY_ID[article.lawId];
	const reviewed = useMizan((s) => s.reviewed[key]);
	const bookmarked = useMizan((s) => s.bookmarks.includes(key));
	const markArticle = useMizan((s) => s.markArticle);
	const toggleBookmark = useMizan((s) => s.toggleBookmark);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("article", {
		className: "rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "flex items-start justify-between gap-3",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
					className: "text-xs text-muted",
					children: [
						law?.shortTitle ?? article.lawId,
						" · ماده ",
						toFaDigits(article.n)
					]
				}), article.heading ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "mt-1 text-base font-medium leading-snug",
					children: article.heading
				}) : null] }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					"aria-label": "نشان‌کردن",
					onClick: () => toggleBookmark(key),
					className: cn("flex size-10 items-center justify-center rounded-md", bookmarked ? "text-accent" : "text-faint"),
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Bookmark, {
						className: "size-4",
						fill: bookmarked ? "currentColor" : "none"
					})
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("blockquote", {
				className: "mt-3 border-r-2 border-accent/40 pr-3 text-sm leading-8",
				children: article.text
			}),
			!compact ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mt-4 space-y-3",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h3", {
						className: "text-xs font-medium text-muted",
						children: "تحلیل و بررسی"
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "mt-1 text-sm leading-7 text-fg",
						children: article.analysis
					})] }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h3", {
						className: "text-xs font-medium text-muted",
						children: "دکترین و صحت‌سنجی"
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "mt-1 text-sm leading-7 text-muted",
						children: article.doctrine
					})] }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
						className: "flex flex-wrap gap-1.5",
						children: article.sources.map((s) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
							variant: "muted",
							children: s
						}, s))
					})
				]
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-3 line-clamp-4 text-sm leading-7 text-muted",
				children: article.analysis
			}),
			article.related.length > 0 && !compact ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
				className: "mt-3 text-xs text-muted",
				children: [
					"مرتبط:",
					" ",
					article.related.map((r, i) => {
						const [lid, num] = r.split(":");
						if (!(lid && num ? getArticle(lid, Number(num)) : void 0) || !lid || !num) return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: [i > 0 ? "، " : null, r] }, r);
						return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: [i > 0 ? "، " : null, /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
							to: "/read/$lawId/$n",
							params: {
								lawId: lid,
								n: num
							},
							className: "text-accent",
							children: ["ماده ", toFaDigits(num)]
						})] }, r);
					})
				]
			}) : null,
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mt-4 flex gap-2",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Button, {
						size: "sm",
						variant: reviewed?.mark === "known" ? "default" : "outline",
						onClick: () => markArticle(key, "known"),
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Check, { className: "size-3.5" }), "ملکه شد"]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Button, {
						size: "sm",
						variant: reviewed?.mark === "again" ? "secondary" : "ghost",
						onClick: () => markArticle(key, "again"),
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(RotateCcw, { className: "size-3.5" }), "تکرار شود"]
					}),
					reviewed ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
						className: "ms-auto self-center text-xs text-faint",
						children: [
							"مرور ",
							toFaDigits(reviewed.count),
							" بار"
						]
					}) : null
				]
			})
		]
	});
}
//#endregion
export { ArticleBlock as t };
