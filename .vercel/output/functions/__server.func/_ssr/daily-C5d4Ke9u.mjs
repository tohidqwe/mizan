import { i as __toESM } from "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { C as TRACK_LAWS, D as todayKey, E as toFaDigits, a as Button, d as useMizan, u as todaysBatch, w as TRACK_META } from "./router-D3fRPlX_.mjs";
import { t as ArticleBlock } from "./article-block-BV72o_Vw.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/daily-C5d4Ke9u.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
function DailyPage() {
	const cursors = useMizan((s) => s.cursors);
	const completeToday = useMizan((s) => s.completeToday);
	const completed = useMizan((s) => s.completedDailyDate);
	const [tab, setTab] = (0, import_react.useState)("civil");
	const done = completed === todayKey();
	const batch = todaysBatch(tab, cursors[tab]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("header", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "سه ماده امروز"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-sm leading-7 text-muted",
				children: "هر روز سه ماده متوالی از قانون مدنی، قانون تجارت و آیین دادرسی مدنی. پس از مرور، «روز تمام شد» را بزنید تا فردا سه ماده بعدی بیاید. اگر هنوز ملکه نشده، «تکرار شود» را بزنید."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "grid grid-cols-3 gap-1 rounded-lg bg-subtle p-1",
				children: TRACK_LAWS.map((t) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: () => setTab(t),
					className: `h-10 rounded-md text-xs font-medium ${tab === t ? "bg-elevated text-fg shadow-[var(--shadow-card)]" : "text-muted"}`,
					children: TRACK_META[t].title.replace("قانون ", "").replace("آیین دادرسی مدنی", "آ.د.م")
				}, t))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
				className: "text-xs text-muted",
				children: [
					"دسته فعلی از ماده ",
					toFaDigits(batch[0]?.n ?? 0),
					" — پس از تکمیل روز، اشاره ",
					toFaDigits(cursors[tab] + 3),
					" ",
					"می‌رود جلو."
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "space-y-3",
				children: batch.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ArticleBlock, { article: a }, `${a.lawId}-${a.n}`))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
				className: "w-full",
				disabled: done,
				onClick: completeToday,
				children: done ? "مرور امروز ثبت شد" : "روز تمام شد؛ فردا سه ماده بعدی"
			})
		]
	});
}
//#endregion
export { DailyPage as component };
