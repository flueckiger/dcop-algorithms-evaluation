package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.Configuration

trait UtilityConfig[AgentId, Action, UtilityType, +Config <: UtilityConfig[AgentId, Action, UtilityType, Config]] extends Configuration[AgentId, Action, Config] {}
