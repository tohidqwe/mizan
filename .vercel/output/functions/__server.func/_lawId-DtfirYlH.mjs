import { a as Trigger2, i as Root2, n as Header, p as require_jsx_runtime, r as Item, t as Content2 } from "./_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "./_libs/@tanstack/react-router+[...].mjs";
import { u as ChevronDown } from "./_libs/lucide-react.mjs";
import { E as toFaDigits, T as cn, b as getLaw, p as ARTICLES_BY_LAW, r as Route$1, x as CATEGORY_LABEL } from "./_ssr/router-D3fRPlX_.mjs";
import { t as Badge } from "./_ssr/badge-Bo9iRCUf.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/_lawId-DtfirYlH.js
var import_jsx_runtime = require_jsx_runtime();
var Accordion = Root2;
function AccordionItem({ className, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Item, {
		className: cn("border-b border-line last:border-b-0", className),
		...props
	});
}
function AccordionTrigger({ className, children, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Header, {
		className: "flex",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Trigger2, {
			className: cn("flex flex-1 items-center justify-between gap-3 py-3 text-right text-sm font-medium transition-opacity hover:opacity-80 [&[data-state=open]>svg]:rotate-180", className),
			...props,
			children: [children, /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ChevronDown, { className: "size-4 shrink-0 text-muted transition-transform duration-200" })]
		})
	});
}
function AccordionContent({ className, children, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Content2, {
		className: "overflow-hidden text-sm",
		...props,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: cn("pb-3 pt-0 text-muted leading-relaxed", className),
			children
		})
	});
}
function LawDetail() {
	const { lawId } = Route$1.useParams();
	const law = getLaw(lawId);
	const articles = ARTICLES_BY_LAW[lawId] ?? [];
	if (!law) return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
			className: "text-xl font-medium",
			children: "قانون یافت نشد"
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
			to: "/laws",
			className: "text-sm text-accent",
			children: "بازگشت به فهرست"
		})]
	});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-5",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("header", {
				className: "space-y-2",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, { children: CATEGORY_LABEL[law.category] }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
						className: "text-2xl font-medium leading-snug",
						children: law.title
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
						className: "text-sm text-muted",
						children: [
							"مصوب ",
							law.year,
							" · حدود ",
							toFaDigits(law.articleCount),
							" ماده"
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "text-sm leading-7",
						children: law.summary
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "منابع معتبر"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
					className: "mt-2 list-disc pr-4 text-sm leading-7 text-muted",
					children: law.sources.map((s) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: s }, s))
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
				className: "mb-2 text-sm font-medium",
				children: "ساختار"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Accordion, {
				type: "single",
				collapsible: true,
				className: "rounded-xl bg-elevated px-4 shadow-[var(--shadow-card)]",
				children: law.chapters.map((ch) => /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(AccordionItem, {
					value: ch.title,
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(AccordionTrigger, { children: ch.title }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(AccordionContent, { children: [
						"مواد ",
						toFaDigits(ch.from),
						" تا ",
						toFaDigits(ch.to)
					] })]
				}, ch.title))
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("h2", {
					className: "text-sm font-medium",
					children: [
						"مواد با تحلیل (",
						toFaDigits(articles.length),
						")"
					]
				}), articles.length === 0 ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "text-sm leading-7 text-muted",
					children: "متن کامل این قانون در سامانه ملی قوانین در دسترس است. برای مرور روزانه، مسیر مدنی / تجارت / آیین دادرسی را دنبال کنید."
				}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
					className: "space-y-2",
					children: articles.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
						to: "/read/$lawId/$n",
						params: {
							lawId,
							n: String(a.n)
						},
						className: "block rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
							className: "text-sm font-medium",
							children: [
								"ماده ",
								toFaDigits(a.n),
								a.heading ? ` — ${a.heading}` : ""
							]
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
							className: "mt-1 line-clamp-2 text-xs leading-6 text-muted",
							children: a.text
						})]
					}) }, a.n))
				})]
			})
		]
	});
}
//#endregion
export { LawDetail as component };
