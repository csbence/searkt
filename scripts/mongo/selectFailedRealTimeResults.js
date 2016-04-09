db.getCollection('experimentResult').find(
  {
    "result.experimentConfiguration.domainName": {$nin: ["ACROBOT"]}
    , "result.experimentConfiguration.algorithmName": {$in: ["RTA_STAR", "LSS_LRTA_STAR"]}
    , "result.success": false
    , "result.errorMessage": {$regex: "Real-time bound is violated*"}
  },
  {
    "result.errorMessage": 1
    , "result.experimentConfiguration.algorithmName": 1
    , "result.experimentConfiguration.domainName": 1

  }
);