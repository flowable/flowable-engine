DECLARE @sql [nvarchar](MAX)
SELECT @sql = N'ALTER TABLE ACT_DMN_DECISION_TABLE DROP CONSTRAINT ' + QUOTENAME([df].[name]) FROM [sys].[columns] AS [c] INNER JOIN [sys].[default_constraints] AS [df] ON [df].[object_id] = [c].[default_object_id] WHERE [c].[object_id] = OBJECT_ID(N'ACT_DMN_DECISION_TABLE') AND [c].[name] = N'PARENT_DEPLOYMENT_ID_'
EXEC sp_executesql @sql;

ALTER TABLE ACT_DMN_DECISION_TABLE DROP COLUMN PARENT_DEPLOYMENT_ID_;
