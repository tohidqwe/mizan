import { p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { v as Link } from "../_libs/@tanstack/react-router+[...].mjs";
import { b as getLaw, h as getArticle, n as Route } from "./router-D3fRPlX_.mjs";
import { t as ArticleBlock } from "./article-block-BV72o_Vw.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/read._lawId._n-D5eolSS3.js
var import_jsx_runtime = require_jsx_runtime();
function ReadArticle() {
	const { lawId, n } = Route.useParams();
	const article = getArticle(lawId, Number(n));
	const law = getLaw(lawId);
	if (!article) return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-3",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h1", {
				className: "text-xl font-medium",
				children: "ماده در بانک تحلیل نیست"
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-sm leading-7 text-muted",
				children: "شماره ماده در فهرست روزانه یا قوانین دارای شرح موجود نیست. از مسیر روزانه به ترتیب شماره پیش بروید."
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Link, {
				to: "/laws/$lawId",
				params: { lawId },
				className: "text-sm text-accent",
				children: ["بازگشت به ", law?.shortTitle ?? "قانون"]
			})
		]
	});
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
		className: "space-y-4",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Link, {
			to: "/laws/$lawId",
			params: { lawId },
			className: "text-xs text-muted",
			children: law?.title
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ArticleBlock, { article })]
	});
}
//#endregion
export { ReadArticle as component };
