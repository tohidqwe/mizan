import { p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link, y as useNavigate } from "../_libs/@tanstack/react-router+[...].mjs";
import { E as toFaDigits, S as SUBJECT_LABEL, a as Button, d as useMizan } from "./router-D3fRPlX_.mjs";
import { t as Badge } from "./badge-Bo9iRCUf.mjs";
import { t as QUESTIONS } from "./questions-C4P-VEBF.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/exam-yuQcf6l0.js
var import_jsx_runtime = require_jsx_runtime();
var SUBJECTS = [
	"civil",
	"commercial",
	"fiqh",
	"pil",
	"procedure"
];
function ExamHome() {
	const picks = useMizan((s) => s.examPicks);
	const answered = Object.keys(picks).length;
	const years = Array.from(new Set(QUESTIONS.map((q) => q.year)));
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-5",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("header", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "دکتری حقوق خصوصی"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-sm leading-7 text-muted",
				children: "مواد امتحانی سازمان سنجش برای این رشته عمدتاً متون فقه معاملات، حقوق مدنی و حقوق تجارت است. آیین دادرسی مدنی و تعارض قوانین به‌عنوان مکمل (مصاحبه و آزمون‌های دانشگاهی) آمده‌اند. پس از هر تست، گزینه درست و علت آن فوراً نشان داده می‌شود."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "rounded-xl bg-elevated p-4 text-sm shadow-[var(--shadow-card)]",
				children: [
					"پاسخ‌داده‌شده:",
					" ",
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
						className: "tabular-nums",
						children: [
							toFaDigits(answered),
							" / ",
							toFaDigits(QUESTIONS.length)
						]
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "شروع سریع"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "grid gap-2",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(StartBtn, {
						n: 10,
						label: "ده تست تصادفی از دروس رسمی",
						subjects: [
							"civil",
							"commercial",
							"fiqh"
						]
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(StartBtn, {
						n: 20,
						label: "بیست تست مخلوط",
						subjects: SUBJECTS
					})]
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "به تفکیک درس"
				}), SUBJECTS.map((s) => {
					const count = QUESTIONS.filter((q) => q.subject === s).length;
					const official = QUESTIONS.some((q) => q.subject === s && q.official);
					return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(StartRow, {
						title: SUBJECT_LABEL[s],
						count,
						badge: official ? "سنجش" : "مکمل",
						subjects: [s]
					}, s);
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "به تفکیک سال"
				}), years.map((y) => {
					const count = QUESTIONS.filter((q) => q.year === y).length;
					return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
						to: "/exam/quiz",
						search: {
							year: y,
							n: count,
							subjects: ""
						},
						className: "flex items-center justify-between rounded-lg bg-elevated px-4 py-3 text-sm shadow-[var(--shadow-card)]",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: ["سال ", y] }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
							className: "text-xs text-muted",
							children: [toFaDigits(count), " سؤال"]
						})]
					}, y);
				})]
			})
		]
	});
}
function StartBtn({ n, label, subjects }) {
	const navigate = useNavigate();
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Button, {
		className: "h-12 w-full justify-between",
		onClick: () => navigate({
			to: "/exam/quiz",
			search: {
				n,
				subjects: subjects.join(","),
				year: ""
			}
		}),
		children: [label, /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
			className: "text-xs opacity-80",
			children: toFaDigits(n)
		})]
	});
}
function StartRow({ title, count, badge, subjects }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
		to: "/exam/quiz",
		search: {
			n: count,
			subjects: subjects.join(","),
			year: ""
		},
		className: "flex items-center justify-between rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "flex items-center gap-2",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
				className: "text-sm",
				children: title
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
				variant: badge === "سنجش" ? "default" : "muted",
				children: badge
			})]
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
			className: "text-xs text-muted",
			children: toFaDigits(count)
		})]
	});
}
//#endregion
export { ExamHome as component };
