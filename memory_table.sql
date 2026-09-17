-- mmapi schema dumped after varchar length optimization
-- source: jdbc:postgresql://10.1.146.70:5433/m5sit?currentSchema=mmapi
-- note: JSON / large payload columns remain TEXT; no data deleted

CREATE SCHEMA IF NOT EXISTS mmapi;
SET search_path TO mmapi;

CREATE TABLE mmapi.base_dict (
    name_snake varchar(128) NOT NULL,
    type varchar(64),
    db_typ varchar(32),
    default_value varchar(64),
    comment_cn varchar(256),
    length bigint,
    status varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT base_dict_pkey PRIMARY KEY (name_snake)
);


CREATE TABLE mmapi.biz_dict (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32) NOT NULL,
    name_camel varchar(128) NOT NULL,
    name_snake varchar(128),
    type varchar(64),
    db_typ varchar(32),
    enum_nme varchar(128),
    enum_ref varchar(256),
    base_ref varchar(16),
    not_null varchar(16),
    default_value varchar(128),
    comment_cn varchar(256),
    comment_en varchar(256),
    length bigint,
    status varchar(8),
    CONSTRAINT biz_dict_pkey PRIMARY KEY (group_name, project_name, app_name, name_camel)
);


CREATE TABLE mmapi.biz_msg_info (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    module_name varchar(64),
    msg_class varchar(64) NOT NULL,
    msg_ref varchar(256) NOT NULL,
    msg_key varchar(128) NOT NULL,
    msg_cd varchar(32),
    msg_desc_cn varchar(256),
    msg_desc_en varchar(256),
    app_name varchar(32) NOT NULL,
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT biz_msg_info_pkey PRIMARY KEY (group_name, project_name, app_name, msg_class, msg_ref, msg_key)
);


CREATE TABLE mmapi.common_class (
    group_name varchar(64) NOT NULL,
    project_name varchar(64),
    module_name varchar(64),
    class_name varchar(128) NOT NULL,
    class_type varchar(32) NOT NULL,
    class_path varchar(256) NOT NULL,
    class_comment_cn varchar(256),
    class_comment_en varchar(256),
    fields_json text,
    completed_flg varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    app_name varchar(32) NOT NULL,
    CONSTRAINT common_class_pkey PRIMARY KEY (group_name, app_name, class_name, class_type, class_path)
);


CREATE TABLE mmapi.db_connection (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32),
    env_name varchar(32) NOT NULL,
    db_name varchar(64),
    driver varchar(64),
    jdbc_url varchar(512),
    username varchar(64),
    password varchar(128),
    schema_nm varchar(64) NOT NULL,
    main_flg varchar(8),
    remark varchar(256),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT db_connection_pkey PRIMARY KEY (group_name, project_name, env_name, schema_nm)
);


CREATE TABLE mmapi.debug_log (
    group_name varchar(64),
    project_name varchar(64),
    app_name varchar(32),
    interface_name varchar(128),
    trans_name varchar(128),
    jrn_no varchar(64) NOT NULL,
    method varchar(16),
    url varchar(256),
    ip varchar(64),
    req_param text,
    rsp_param text,
    headers text,
    msg_code varchar(16),
    msg_inf varchar(1024),
    debug_desc varchar(256),
    sequence varchar(128) NOT NULL,
    update_by varchar(64),
    update_time varchar(64),
    request_id varchar(128),
    trans_nme_dsc varchar(32),
    CONSTRAINT debug_log_pkey PRIMARY KEY (sequence)
);


CREATE TABLE mmapi.enum_dict (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    module_name varchar(64),
    enum_nme varchar(128) NOT NULL,
    enum_ref varchar(256) NOT NULL,
    enum_cd varchar(128),
    enum_val varchar(128) NOT NULL,
    desc_cn varchar(256),
    desc_en varchar(256),
    db_name varchar(64),
    app_name varchar(32) NOT NULL,
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT enum_dict_pkey PRIMARY KEY (group_name, project_name, app_name, enum_nme, enum_ref, enum_val)
);


CREATE TABLE mmapi.enum_dict_his (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    module_name varchar(64),
    enum_nme varchar(128) NOT NULL,
    enum_ref varchar(256) NOT NULL,
    enum_cd varchar(128),
    enum_val varchar(128) NOT NULL,
    desc_cn varchar(256),
    desc_en varchar(256),
    db_name varchar(64),
    app_name varchar(32) NOT NULL,
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT enum_dict_his_pkey PRIMARY KEY (group_name, project_name, app_name, enum_nme, enum_ref, enum_val)
);


CREATE TABLE mmapi.exesql_record (
    id bigint NOT NULL,
    group_name varchar(64),
    schema_nm varchar(64),
    env_name varchar(32),
    exe_date varchar(32),
    exe_time varchar(32),
    exe_sql text,
    update_by varchar(64),
    update_time varchar(64),
    status varchar(8),
    CONSTRAINT exesql_record_pkey PRIMARY KEY (id)
);


CREATE TABLE mmapi.interface_data (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    interface_name varchar(128),
    trans_name varchar(128) NOT NULL,
    class_name varchar(256) NOT NULL,
    module_name varchar(128),
    trans_comment_zh varchar(256),
    trans_comment_en varchar(256),
    trans_class varchar(64),
    simple_name varchar(128),
    request_json text,
    response_json text,
    request_package varchar(256),
    response_package varchar(256),
    request_class varchar(128),
    response_class varchar(128),
    interface_url varchar(256),
    method_url varchar(256),
    update_by varchar(64),
    update_time varchar(64),
    app_name varchar(32) NOT NULL,
    lable_name varchar(64),
    associat_entity varchar(512),
    associat_enum varchar(512),
    req_parent_class varchar(256),
    rsp_parent_class varchar(256),
    CONSTRAINT interface_data_pkey PRIMARY KEY (group_name, project_name, app_name, class_name, trans_name)
);


CREATE TABLE mmapi.interface_data_his (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32) NOT NULL,
    interface_name varchar(128),
    trans_name varchar(128) NOT NULL,
    class_name varchar(256) NOT NULL,
    trans_comment_zh varchar(256),
    trans_comment_en varchar(256),
    interface_url varchar(256),
    method_url varchar(256),
    lable_name varchar(64),
    CONSTRAINT interface_data_his_pkey PRIMARY KEY (group_name, project_name, app_name, class_name, trans_name)
);


CREATE TABLE mmapi.login_info (
    id varchar(8) NOT NULL,
    username varchar(64) NOT NULL,
    password varchar(128) NOT NULL,
    display_name varchar(64),
    email varchar(128),
    phone varchar(32),
    role varchar(8) DEFAULT 'user'::text,
    status varchar(16) DEFAULT 'active'::text,
    last_login_time varchar(64),
    last_login_ip varchar(64),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT login_info_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_login_info_status ON mmapi.login_info USING btree (status);
CREATE INDEX idx_login_info_username ON mmapi.login_info USING btree (username);
CREATE UNIQUE INDEX uk_login_info_username ON mmapi.login_info USING btree (username);

CREATE TABLE mmapi.project_folder (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32),
    module_name varchar(64) NOT NULL,
    dir_base varchar(64),
    dir_type varchar(32) NOT NULL,
    dir_path varchar(256),
    main_flg varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT project_folder_pkey PRIMARY KEY (group_name, project_name, module_name, dir_type)
);


CREATE TABLE mmapi.project_group (
    group_name varchar(64) NOT NULL,
    group_desc varchar(256),
    cur_flag varchar(8),
    CONSTRAINT project_group_pkey PRIMARY KEY (group_name)
);


CREATE TABLE mmapi.project_setting (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    project_type varchar(32),
    project_desc varchar(256),
    app_name varchar(32) NOT NULL,
    app_port varchar(16),
    schema_nm varchar(64),
    cur_flag varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    show_flag varchar(8),
    CONSTRAINT project_setting_pkey PRIMARY KEY (group_name, project_name, app_name)
);


CREATE TABLE mmapi.role_menu (
    role_code varchar(8) NOT NULL,
    menu_key varchar(64) NOT NULL,
    menu_name varchar(64),
    group_key varchar(64),
    group_name varchar(64),
    sort_no integer,
    show_flag varchar(8) DEFAULT 'Y'::character varying,
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT role_menu_pkey PRIMARY KEY (role_code, menu_key)
);

CREATE INDEX idx_role_menu_role ON mmapi.role_menu USING btree (role_code);

CREATE TABLE mmapi.server_info (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32) NOT NULL,
    env_name varchar(32),
    app_port varchar(16),
    ip varchar(64) NOT NULL,
    port varchar(8),
    username varchar(64),
    password varchar(128),
    app_path varchar(256),
    app_prop varchar(128),
    update_by varchar(64),
    update_time varchar(64),
    scan_flg bigint DEFAULT 1,
    CONSTRAINT server_info_pkey PRIMARY KEY (group_name, project_name, app_name, ip)
);


CREATE TABLE mmapi.table_data (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    app_name varchar(32) NOT NULL,
    module_name varchar(64),
    table_name_camel varchar(128) NOT NULL,
    table_name_snake varchar(128),
    table_comment_cn varchar(256),
    table_comment_en varchar(256),
    fields_json text,
    primary_key_json text,
    indexes_json text,
    lable_name varchar(64),
    associat_enum varchar(64),
    parent_class varchar(64),
    gener_cd_flg varchar(8),
    create_tab_flg varchar(8),
    def_order_by varchar(32),
    status varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT table_data_pkey PRIMARY KEY (group_name, project_name, app_name, table_name_camel)
);


CREATE TABLE mmapi.table_diff (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    table_name_camel varchar(128) NOT NULL,
    schema_nm varchar(64) NOT NULL,
    compare_env varchar(32) NOT NULL,
    app_name varchar(32),
    table_name_snake varchar(128),
    diff_type varchar(32),
    local_value text,
    remote_value text,
    diff_field_count bigint DEFAULT 0,
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT table_diff_pkey PRIMARY KEY (group_name, project_name, table_name_camel, schema_nm, compare_env)
);

CREATE INDEX idx_table_diff_app_name ON mmapi.table_diff USING btree (app_name);
CREATE INDEX idx_table_diff_diff_type ON mmapi.table_diff USING btree (diff_type);
CREATE INDEX idx_table_diff_table_snake ON mmapi.table_diff USING btree (table_name_snake);
CREATE INDEX idx_table_diff_update_time ON mmapi.table_diff USING btree (update_time);

CREATE TABLE mmapi.table_record (
    id varchar(64) NOT NULL,
    group_name varchar(64),
    project_name varchar(64),
    app_name varchar(32),
    module_name varchar(64),
    table_name_camel varchar(128),
    table_name_snake varchar(128),
    table_comment_cn varchar(256),
    table_comment_en varchar(256),
    fields_json text,
    primary_key_json text,
    indexes_json text,
    lable_name varchar(64),
    associat_enum varchar(64),
    parent_class varchar(64),
    gener_cd_flg varchar(8),
    create_tab_flg varchar(8),
    def_order_by varchar(64),
    execsql_json text,
    status varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT table_record_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_table_record_app_name ON mmapi.table_record USING btree (app_name);
CREATE INDEX idx_table_record_group_name ON mmapi.table_record USING btree (group_name);
CREATE INDEX idx_table_record_group_project ON mmapi.table_record USING btree (group_name, project_name);
CREATE INDEX idx_table_record_project_app ON mmapi.table_record USING btree (project_name, app_name);
CREATE INDEX idx_table_record_project_name ON mmapi.table_record USING btree (project_name);
CREATE INDEX idx_table_record_status ON mmapi.table_record USING btree (status);
CREATE INDEX idx_table_record_table_name_snake ON mmapi.table_record USING btree (table_name_snake);
CREATE INDEX idx_table_record_update_time ON mmapi.table_record USING btree (update_time);

CREATE TABLE mmapi.user_info (
    user_id varchar(64) NOT NULL,
    username varchar(64) NOT NULL,
    password varchar(128) NOT NULL,
    real_name varchar(128) NOT NULL,
    email varchar(128),
    phone varchar(32),
    roles varchar(8) NOT NULL,
    status varchar(8) NOT NULL,
    create_by varchar(64),
    create_time varchar(64),
    update_by varchar(64),
    update_time varchar(64),
    remark varchar(256),
    CONSTRAINT user_info_pkey PRIMARY KEY (user_id)
);

CREATE INDEX idx_user_info_email ON mmapi.user_info USING btree (email);
CREATE INDEX idx_user_info_role ON mmapi.user_info USING btree (roles);
CREATE INDEX idx_user_info_username ON mmapi.user_info USING btree (username);
CREATE UNIQUE INDEX uk_user_info_username ON mmapi.user_info USING btree (username);

CREATE TABLE mmapi.user_proj_group (
    group_name varchar(64) NOT NULL,
    group_desc varchar(256),
    user_id varchar(64) NOT NULL,
    username varchar(64) NOT NULL,
    cur_flag varchar(8),
    CONSTRAINT user_proj_group_pkey PRIMARY KEY (group_name, user_id, username)
);


CREATE TABLE mmapi.user_proj_setting (
    group_name varchar(64) NOT NULL,
    project_name varchar(64) NOT NULL,
    project_type varchar(32),
    project_desc varchar(256),
    app_name varchar(32) NOT NULL,
    app_port varchar(16),
    user_id varchar(64) NOT NULL,
    username varchar(64) NOT NULL,
    schema_nm varchar(64),
    base_path varchar(256),
    cur_flag varchar(8),
    show_flag varchar(8),
    update_by varchar(64),
    update_time varchar(64),
    CONSTRAINT user_proj_setting_pkey PRIMARY KEY (group_name, project_name, app_name, user_id, username)
);
