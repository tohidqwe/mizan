import "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { a as Scale, c as GraduationCap, h as ArrowLeft, m as Bell, p as BookOpen } from "../_libs/lucide-react.mjs";
import { C as TRACK_LAWS, D as todayKey, E as toFaDigits, T as cn, d as useMizan, f as ALL_ARTICLES, u as todaysBatch, v as LAWS, w as TRACK_META } from "./router-D3fRPlX_.mjs";
import { t as Badge } from "./badge-Bo9iRCUf.mjs";
import { t as QUESTIONS } from "./questions-C4P-VEBF.mjs";
import { t as Progress } from "./progress-Cvorm2YL.mjs";
require_react();
var import_jsx_runtime = require_jsx_runtime();
function Card({ className, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: cn("rounded-xl bg-elevated text-fg shadow-[var(--shadow-card)]", className),
		...props
	});
}
function CardContent({ className, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: cn("p-4", className),
		...props
	});
}
function Home() {
	const cursors = useMizan((s) => s.cursors);
	const reviewed = useMizan((s) => s.reviewed);
	const examPicks = useMizan((s) => s.examPicks);
	const completedDailyDate = useMizan((s) => s.completedDailyDate);
	const known = Object.values(reviewed).filter((r) => r.mark === "known").length;
	const answered = Object.keys(examPicks).length;
	const doneToday = completedDailyDate === todayKey();
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-6",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", { children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "text-xs font-medium tracking-wide text-muted",
					children: "دانشنامه و آزمون"
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
					className: "mt-1 text-3xl font-medium leading-tight",
					children: "میزان"
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "mt-2 max-w-prose text-sm leading-7 text-muted",
					children: "قوانین ایران با متن رسمی، تحلیل دکترین و صحت‌سنجی؛ به‌علاوه بانک تست دکتری حقوق خصوصی با پاسخ تشریحی. هر روز سه ماده از مدنی، تجارت و آیین دادرسی، به ترتیب شماره، تا ملکه ذهن شود."
				})
			] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "grid grid-cols-3 gap-2",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Stat, {
						label: "قوانین",
						value: toFaDigits(LAWS.length)
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Stat, {
						label: "مواد تحلیل‌شده",
						value: toFaDigits(ALL_ARTICLES.length)
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Stat, {
						label: "تست دکتری",
						value: toFaDigits(QUESTIONS.length)
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Card, { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(CardContent, {
				className: "space-y-3",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "flex items-center justify-between",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
							className: "text-sm font-medium",
							children: "مرور امروز"
						}), doneToday ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
							variant: "ok",
							children: "ثبت شد"
						}) : /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Badge, {
							variant: "muted",
							children: "در انتظار مرور"
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
						className: "space-y-2",
						children: TRACK_LAWS.map((t) => {
							const batch = todaysBatch(t, cursors[t]);
							return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
								className: "flex items-baseline justify-between gap-3 text-sm",
								children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
									className: "text-muted",
									children: TRACK_META[t].title
								}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
									className: "font-medium tabular-nums",
									children: ["مواد ", batch.map((a) => toFaDigits(a.n)).join("، ")]
								})]
							}, t);
						})
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
						to: "/daily",
						className: "flex h-11 items-center justify-center gap-2 rounded-md bg-accent text-sm font-medium text-accent-fg",
						children: ["شروع مرور روزانه", /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ArrowLeft, { className: "size-4" })]
					})
				]
			}) }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "grid gap-3 sm:grid-cols-2",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(NavCard, {
						to: "/laws",
						icon: Scale,
						title: "قوانین ایران",
						text: "اساسی، مدنی، تجارت، کیفری، کار، خانواده، ثبت و ده‌ها قانون خاص با ساختار و منابع."
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(NavCard, {
						to: "/exam",
						icon: GraduationCap,
						title: "دکتری حقوق خصوصی",
						text: "تست‌های مدنی، تجارت، متون فقه معاملات و حقوق بین‌الملل خصوصی با علت پاسخ."
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(NavCard, {
						to: "/reminders",
						icon: Bell,
						title: "یادآوری روزانه",
						text: "ساعت دلخواه را بگذارید؛ پوش نوتیفیکیشن مواد و تست را تکرار می‌کند."
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(NavCard, {
						to: "/search",
						icon: BookOpen,
						title: "جستجوی مواد",
						text: "متن ماده، شماره یا کلیدواژه را بجویید."
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-2",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-sm font-medium",
					children: "پیشرفت شما روی همین دستگاه"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "mb-2 flex justify-between text-xs text-muted",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: "مواد ملکه‌شده" }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
								className: "tabular-nums",
								children: toFaDigits(known)
							})]
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Progress, { value: Math.min(100, known * 4) }),
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "mt-4 mb-2 flex justify-between text-xs text-muted",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: "تست پاسخ‌داده‌شده" }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
								className: "tabular-nums",
								children: [
									toFaDigits(answered),
									" از ",
									toFaDigits(QUESTIONS.length)
								]
							})]
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Progress, { value: answered / QUESTIONS.length * 100 })
					]
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-xs leading-6 text-faint",
				children: "منابع متن: سامانه ملی قوانین و مقررات، مرکز پژوهش‌های مجلس، روزنامه رسمی. تحلیل‌ها بر پایه دکترین معتبر (کاتوزیان، امامی، صفایی، شهیدی، شمس، اسکینی، کاویانی) و آرای وحدت‌رویه است. این برنامه مشاوره وکالت نیست."
			})
		]
	});
}
function Stat({ label, value }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "rounded-lg bg-elevated px-3 py-3 text-center shadow-[var(--shadow-card)]",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "text-lg font-medium tabular-nums",
			children: value
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "text-xs text-muted",
			children: label
		})]
	});
}
function NavCard({ to, icon: Icon, title, text }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
		to,
		className: "block rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Icon, {
				className: "size-5 text-accent",
				strokeWidth: 1.6
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h3", {
				className: "mt-3 text-sm font-medium",
				children: title
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-xs leading-6 text-muted",
				children: text
			})
		]
	});
}
//#endregion
export { Home as component };
