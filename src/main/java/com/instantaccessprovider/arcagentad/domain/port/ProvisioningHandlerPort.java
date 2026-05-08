package com.instantaccessprovider.arcagentad.domain.port;

import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;

public interface ProvisioningHandlerPort {
    void execute(ProvisioningCommand command);
}
