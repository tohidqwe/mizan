import { i as __toESM } from "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { E as toFaDigits, S as SUBJECT_LABEL, T as cn, a as Button, d as useMizan, i as Route$3 } from "./router-D3fRPlX_.mjs";
import { t as Badge } from "./badge-Bo9iRCUf.mjs";
import { t as QUESTIONS } from "./questions-C4P-VEBF.mjs";
import { t as Progress } from "./progress-Cvorm2YL.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/quiz-ZYUbWmsK.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
function pickQuestions(n, subjects, year) {
	let pool = QUESTIONS;
	if (year) pool = pool.filter((q) => q.year === year);
	const subj = subjects.split(",").map((x) => x.trim()).filter(Boolean);
	if (subj.length) pool = pool.filter((q) => subj.includes(q.subject));
	const shuffled = [...pool].sort(() => Math.random() - .5);
	const count = Math.min(n || shuffled.length, shuffled.length);
	return shuffled.slice(0, count);
}
function QuizPage() {
	const search = Route$3.useSearch();
	const n = search.n ?? 10;
	const subjects = search.subjects ?? "";
	const year = search.year ?? "";
	const recordExam = useMizan((s) => s.recordExam);
	const [set, setSet] = (0, import_react.useState)(null);
	const [i, setI] = (0, import_react.useState)(0);
	const [choice, setChoice] = (0, import_react.useState)(null);
	const [revealed, setRevealed] = (0, import_react.useState)(false);
	const [score, setScore] = (0, import_react.useState)(0);
	const [done, setDone] = (0, import_react.useState)(false);
	(0, import_react.useEffect)(() => {
		setSet(pickQuestions(n, subjects, year));
		setI(0);
		setChoice(null);
		setRevealed(false);
		setScore(0);
		setDone(false);
	}, [
		n,
		subjects,
		year
	]);
	if (!set) return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
		className: "text-sm text-muted",
		children: "در حال آماده‌سازی پرسش‌ها…"
	});
	if (!set.length) return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
			className: "text-xl font-medium",
			children: "سؤالی در این فیلتر نیست"
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
			to: "/exam",
			className: "text-sm text-accent",
			children: "بازگشت"
		})]
	});
	const q = set[i];
	if (done) return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "پایان آزمون"
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
				className: "text-sm text-muted",
				children: [
					"پاسخ درست: ",
					toFaDigits(score),
					" از ",
					toFaDigits(set.length)
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Progress, { value: score / set.length * 100 }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "flex gap-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
					to: "/exam",
					className: "flex h-11 flex-1 items-center justify-center rounded-md bg-accent text-sm text-accent-fg",
					children: "بانک سؤال"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
					variant: "outline",
					className: "flex-1",
					onClick: () => {
						setSet(pickQuestions(n, subjects, year));
						setI(0);
						setChoice(null);
						setRevealed(false);
						setScore(0);
						setDone(false);
					},
					children: "ست جدید"
				})]
			})
		]
	});
	function submit(pick) {
		if (revealed) return;
		setChoice(pick);
		setRevealed(true);
		recordExam(q.id, pick);
		if (pick === q.answer) setScore((s) => s + 1);
	}
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "flex items-center justify-between text-xs text-muted",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", { children: [
					"سؤال ",
					toFaDigits(i + 1),
					" از ",
					toFaDigits(set.length)
				] }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
					className: "tabular-nums",
					children: ["درست: ", toFaDigits(score)]
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Progress, { value: (i + (revealed ? 1 : 0)) / set.length * 100 }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("article", {
				className: "rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "mb-3 flex flex-wrap gap-1.5",
						children: [
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, { children: SUBJECT_LABEL[q.subject] }),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
								variant: "muted",
								children: q.year
							}),
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
								variant: "muted",
								children: q.topic
							})
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
						className: "text-base font-medium leading-8",
						children: q.stem
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
						className: "mt-4 space-y-2",
						children: q.choices.map((c, idx) => {
							const isPick = choice === idx;
							const isAns = q.answer === idx;
							const show = revealed;
							return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
								type: "button",
								disabled: revealed,
								onClick: () => submit(idx),
								className: cn("w-full rounded-lg px-3 py-3 text-right text-sm leading-7 shadow-[0_0_0_1px_var(--mizan-line)]", !show && "hover:bg-subtle", show && isAns && "bg-ok/12 shadow-[0_0_0_1px_var(--mizan-ok)]", show && isPick && !isAns && "bg-seal/10 shadow-[0_0_0_1px_var(--mizan-seal)]"),
								children: [
									/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
										className: "ml-2 text-xs text-muted",
										children: [[
											"الف",
											"ب",
											"ج",
											"د"
										][idx], "."]
									}),
									" ",
									c
								]
							}) }, `${q.id}-${idx}`);
						})
					})
				]
			}),
			revealed ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: cn("text-sm font-medium", choice === q.answer ? "text-ok" : "text-seal"),
						children: choice === q.answer ? "پاسخ شما درست است" : `پاسخ درست: ${[
							"الف",
							"ب",
							"ج",
							"د"
						][q.answer]}`
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
						className: "mt-3 text-xs font-medium text-muted",
						children: "علت صحت گزینه"
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "mt-1 text-sm leading-7",
						children: q.explanation
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
						className: "mt-3 text-xs text-muted",
						children: ["مستند: ", q.articles.join(" · ")]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
						className: "mt-4 w-full",
						onClick: () => {
							if (i + 1 >= set.length) {
								setDone(true);
								return;
							}
							setI(i + 1);
							setChoice(null);
							setRevealed(false);
						},
						children: i + 1 >= set.length ? "مشاهده نتیجه" : "سؤال بعد"
					})
				]
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-xs text-muted",
				children: "یک گزینه را بزنید تا پاسخ و علت نمایش داده شود."
			})
		]
	});
}
//#endregion
export { QuizPage as component };
