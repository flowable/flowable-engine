alter table ACT_HI_PROCINST
	add NAME_ varchar(191);
	
Call Sysproc.admin_cmd ('REORG TABLE ACT_HI_PROCINST');
