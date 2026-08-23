import { p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { T as cn } from "./router-D3fRPlX_.mjs";
import { n as Root, t as Indicator } from "../_libs/radix-ui__react-progress.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/progress-Cvorm2YL.js
var import_jsx_runtime = require_jsx_runtime();
function Progress({ className, value = 0, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Root, {
		className: cn("relative h-1.5 w-full overflow-hidden rounded-full bg-subtle", className),
		...props,
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Indicator, {
			className: "h-full bg-accent transition-[width] duration-300",
			style: { width: `${value}%` }
		})
	});
}
//#endregion
export { Progress as t };
