import { i as __toESM } from "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { E as toFaDigits, _ as searchLaws, g as searchArticles } from "./router-D3fRPlX_.mjs";
import { t as Input } from "./input-Ctv1-vQz.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/search-VZ0eWXgT.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
function SearchPage() {
	const [q, setQ] = (0, import_react.useState)("");
	const laws = (0, import_react.useMemo)(() => q.trim() ? searchLaws(q) : [], [q]);
	const articles = (0, import_react.useMemo)(() => searchArticles(q), [q]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "جستجو"
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Input, {
				autoFocus: true,
				value: q,
				onChange: (e) => setQ(e.target.value),
				placeholder: "شماره ماده، عبارت قانونی یا نام قانون",
				"aria-label": "عبارت جستجو"
			}),
			q.trim().length < 2 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-sm text-muted",
				children: "دست‌کم دو نویسه بنویسید."
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(import_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "قوانین"
				}), laws.length === 0 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "text-sm text-muted",
					children: "موردی نیست."
				}) : laws.slice(0, 8).map((l) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
					to: "/laws/$lawId",
					params: { lawId: l.id },
					className: "block rounded-lg bg-elevated px-4 py-3 text-sm shadow-[var(--shadow-card)]",
					children: l.title
				}, l.id))]
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "مواد"
				}), articles.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
					to: "/read/$lawId/$n",
					params: {
						lawId: a.lawId,
						n: String(a.n)
					},
					className: "block rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
						className: "text-sm font-medium",
						children: [
							"ماده ",
							toFaDigits(a.n),
							" — ",
							a.heading ?? a.lawId
						]
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "mt-1 line-clamp-2 text-xs leading-6 text-muted",
						children: a.text
					})]
				}, `${a.lawId}-${a.n}`))]
			})] })
		]
	});
}
//#endregion
export { SearchPage as component };
