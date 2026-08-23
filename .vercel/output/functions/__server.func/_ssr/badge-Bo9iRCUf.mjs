import { t as cva } from "../_libs/class-variance-authority+clsx.mjs";
import { p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { T as cn } from "./router-D3fRPlX_.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/badge-Bo9iRCUf.js
var import_jsx_runtime = require_jsx_runtime();
var badgeVariants = cva("inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium", {
	variants: { variant: {
		default: "bg-tint text-accent",
		muted: "bg-subtle text-muted",
		ok: "bg-ok/12 text-ok",
		seal: "bg-seal/12 text-seal",
		warn: "bg-warn/12 text-warn"
	} },
	defaultVariants: { variant: "default" }
});
function Badge({ className, variant, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
		className: cn(badgeVariants({
			variant,
			className
		})),
		...props
	});
}
//#endregion
export { Badge as t };
