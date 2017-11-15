select networkName
  from SystemInfoNetworks
  where systemInfoUuid = ?
  order by ordinal;