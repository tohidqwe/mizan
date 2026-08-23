import { i as __toESM } from "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { T as cn, a as Button, c as initNotifications, d as useMizan, l as scheduleReminderLoop, o as ensurePermission, s as fireTodaysNotifications } from "./router-D3fRPlX_.mjs";
import { n as SwitchThumb, t as Switch$1 } from "../_libs/@radix-ui/react-switch+[...].mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/reminders-CSqVDrAR.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
function Switch({ className, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Switch$1, {
		dir: "ltr",
		className: cn("peer inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full bg-subtle shadow-[0_0_0_1px_var(--mizan-line)] transition-colors data-[state=checked]:bg-accent", className),
		...props,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SwitchThumb, { className: "pointer-events-none block size-5 translate-x-0.5 rounded-full bg-elevated shadow-sm transition-transform data-[state=checked]:translate-x-[22px]" })
	});
}
function RemindersPage() {
	const reminder = useMizan((s) => s.reminder);
	const setReminder = useMizan((s) => s.setReminder);
	const [perm, setPerm] = (0, import_react.useState)("default");
	const [msg, setMsg] = (0, import_react.useState)("");
	(0, import_react.useEffect)(() => {
		if ("Notification" in window) setPerm(Notification.permission);
	}, []);
	(0, import_react.useEffect)(() => {
		scheduleReminderLoop();
	}, [reminder.time, reminder.enabled]);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-5",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("header", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-2xl font-medium",
				children: "یادآوری"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-sm leading-7 text-muted",
				children: "ساعت را مشخص کنید. هر روز در همان ساعت، سه ماده مدنی، سه ماده تجارت و سه ماده آیین دادرسی — به ترتیب شماره — و یک یادآوری تست دکتری برایتان اعلان می‌شود. تکرار می‌شود تا ملکه ذهن شود."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-4 rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Row, {
						title: "فعال بودن یادآوری",
						desc: "زمان‌بندی روی همین دستگاه ذخیره می‌شود.",
						checked: reminder.enabled,
						onCheckedChange: (v) => setReminder({ enabled: v })
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "flex items-center justify-between gap-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
							className: "text-sm font-medium",
							children: "ساعت اعلان"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
							className: "text-xs text-muted",
							children: "۲۴ساعته، به وقت دستگاه شما"
						})] }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
							type: "time",
							value: reminder.time,
							onChange: (e) => setReminder({ time: e.target.value || "08:00" }),
							className: "h-11 rounded-md bg-subtle px-3 text-sm text-fg"
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Row, {
						title: "مواد قانونی روزانه",
						desc: "مدنی + تجارت + آیین دادرسی مدنی",
						checked: reminder.articles,
						onCheckedChange: (v) => setReminder({ articles: v })
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Row, {
						title: "یادآوری تست‌زنی دکتری",
						desc: "یک نوبت در روز برای بانک سؤال",
						checked: reminder.exam,
						onCheckedChange: (v) => setReminder({ exam: v })
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("section", {
				className: "space-y-3 rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
						className: "text-sm font-medium",
						children: "مجوز پوش نوتیفیکیشن"
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
						className: "text-xs leading-6 text-muted",
						children: [
							"وضعیت فعلی: ",
							permLabel(perm),
							". اپ را به صفحه اصلی اندروید اضافه کنید، سپس مجوز اعلان را بدهید. مرورگر اگر برنامه را کاملاً ببندد ممکن است اعلان زمان‌بندی‌شده را به تأخیر بیندازد؛ با باز کردن میزان، مطالب همان روز آماده است."
						]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "flex flex-col gap-2",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							onClick: async () => {
								const p = await ensurePermission();
								setPerm(p);
								if (p === "granted") {
									await initNotifications();
									setMsg("مجوز داده شد. اعلان‌ها در ساعت تنظیم‌شده ارسال می‌شوند.");
								} else if (p === "denied") setMsg("مرورگر مجوز را رد کرده؛ از تنظیمات سایت آن را فعال کنید.");
								else setMsg("این مرورگر اعلان وب را پشتیبانی نمی‌کند.");
							},
							children: "درخواست مجوز اعلان"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Button, {
							variant: "outline",
							onClick: async () => {
								useMizan.setState({ lastNotifyDate: null });
								await fireTodaysNotifications();
								setMsg("اعلان آزمایشی ارسال شد (اگر مجوز داده باشید).");
							},
							children: "ارسال اعلان آزمایشی همین حالا"
						})]
					}),
					msg ? /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "text-xs text-ok",
						children: msg
					}) : null
				]
			})
		]
	});
}
function Row({ title, desc, checked, onCheckedChange }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "flex items-center justify-between gap-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
			className: "text-sm font-medium",
			children: title
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
			className: "text-xs text-muted",
			children: desc
		})] }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Switch, {
			checked,
			onCheckedChange
		})]
	});
}
function permLabel(p) {
	if (p === "granted") return "مجاز";
	if (p === "denied") return "مسدود";
	return "هنوز پرسیده نشده";
}
//#endregion
export { RemindersPage as component };
