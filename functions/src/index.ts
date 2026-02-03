import {setGlobalOptions} from "firebase-functions";

// Set default region and memory to save costs
setGlobalOptions({
  region: "europe-west1",
  memory: "256MiB",
  maxInstances: 10,
});

import {onExpenseWrite} from "./triggers/onExpenseWrite";
import {onUserUpdate} from "./triggers/onUserUpdate";
import {onGroupWrite} from "./triggers/onGroupWrite";
import {createInviteCode, joinByInviteCode} from "./callable/invites";
import {claimGhost} from "./callable/smartLinking";

// Export all functions
export {
  onExpenseWrite,
  onUserUpdate,
  onGroupWrite,
  createInviteCode,
  joinByInviteCode,
  claimGhost,
};
