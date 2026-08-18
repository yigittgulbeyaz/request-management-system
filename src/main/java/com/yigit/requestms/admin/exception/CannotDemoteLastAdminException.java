package com.yigit.requestms.admin.exception;

import com.yigit.requestms.common.exception.BaseException;

// An account system with nobody able to administer it cannot be repaired from
// inside itself. The last administrator is the one account the rules protect
// from its own holder.
public class CannotDemoteLastAdminException extends BaseException {

    public CannotDemoteLastAdminException() {
        super("LAST_ADMIN_PROTECTED",
                "The only remaining administrator cannot be demoted or deactivated");
    }
}