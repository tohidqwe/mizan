import { i as __toESM } from "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { E as toFaDigits, v as LAWS, x as CATEGORY_LABEL } from "./router-D3fRPlX_.mjs";
import { t as Badge } from "./badge-Bo9iRCUf.mjs";
import { t as Input } from "./input-Ctv1-vQz.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/laws-CUJC-2GF.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
var CATS = ["all", ...Object.keys(CATEGORY_LABEL)];
function LawsPage() {
	const [q, setQ] = (0, import_react.useState)("");
	const [cat, setCat] = (0, import_react.useState)("all");
	const list = (0, import_react.useMemo)(() => {
		return LAWS.filter((l) => {
			if (cat !== "all" && l.category !== cat) return false;
			if (!q.trim()) return true;
			return l.title.includes(q) || l.shortTitle.includes(q) || l.summary.includes(q);
		});
	}, [q, cat]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("header", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "قوانین ایران"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-sm leading-7 text-muted",
				children: "فهرست شاخه‌های اصلی نظام حقوقی ایران با ساختار مواد و منابع معتبر. روی هر قانون بزنید تا مواد تحلیل‌شده را بخوانید."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Input, {
				value: q,
				onChange: (e) => setQ(e.target.value),
				placeholder: "جستجوی عنوان قانون…",
				"aria-label": "جستجوی قانون"
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "flex gap-1.5 overflow-x-auto pb-1",
				children: CATS.map((c) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: () => setCat(c),
					className: `h-9 shrink-0 rounded-full px-3 text-xs ${cat === c ? "bg-accent text-accent-fg" : "bg-subtle text-muted"}`,
					children: c === "all" ? "همه" : CATEGORY_LABEL[c]
				}, c))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
				className: "space-y-2",
				children: list.map((law) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
					to: "/laws/$lawId",
					params: { lawId: law.id },
					className: "block rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "flex items-start justify-between gap-3",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
								className: "text-sm font-medium leading-snug",
								children: law.title
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
								variant: "muted",
								children: CATEGORY_LABEL[law.category]
							})]
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
							className: "mt-1 text-xs text-muted",
							children: [
								"مصوب ",
								law.year,
								" · ",
								toFaDigits(law.articleCount),
								" ماده"
							]
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
							className: "mt-2 line-clamp-2 text-xs leading-6 text-muted",
							children: law.summary
						})
					]
				}) }, law.id))
			})
		]
	});
}
//#endregion
export { LawsPage as component };
