// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcagentad.domain.port;

import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;

public interface ProvisioningHandlerPort {
    void execute(ProvisioningCommand command, String topic);
}
