import "../_runtime.mjs";
import { m as require_react, p as require_jsx_runtime } from "../_libs/@radix-ui/react-accordion+[...].mjs";
import { T as cn } from "./router-D3fRPlX_.mjs";
require_react();
var import_jsx_runtime = require_jsx_runtime();
function Input({ className, ...props }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
		className: cn("flex h-11 w-full rounded-md bg-elevated px-3 text-sm text-fg shadow-[0_0_0_1px_var(--mizan-line)] placeholder:text-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/35 disabled:opacity-50", className),
		...props
	});
}
//#endregion
export { Input as t };
