create table biz_msg_info
(
    group_name   text not null,
    project_name text not null,
    module_name  text,
    msg_class    text not null,
    msg_ref      text not null,
    msg_key      text not null,
    msg_cd       text,
    msg_desc_cn  text,
    msg_desc_en  text,
    app_name     text not null,
    update_by    text,
    update_time  text,
    primary key (group_name, project_name, app_name, msg_class, msg_ref, msg_key)
);

alter table biz_msg_info
    owner to coretsdapi;

create table common_class
(
    group_name       text not null,
    project_name     text,
    module_name      text,
    class_name       text not null,
    class_type       text not null,
    class_path       text not null,
    class_comment_cn text,
    class_comment_en text,
    fields_json      text,
    completed_flg    text,
    update_by        text,
    update_time      text,
    app_name         text not null,
    primary key (group_name, app_name, class_name, class_type, class_path)
);

alter table common_class
    owner to coretsdapi;

create table db_connection
(
    group_name   text not null,
    project_name text not null,
    app_name     text,
    env_name     text not null,
    db_name      text,
    driver       text,
    jdbc_url     text,
    username     text,
    password     text,
    schema_nm    text not null,
    main_flg     text,
    remark       text,
    update_by    text,
    update_time  text,
    primary key (group_name, project_name, env_name, schema_nm)
);

alter table db_connection
    owner to coretsdapi;

create table debug_log
(
    group_name     text,
    project_name   text,
    app_name       text,
    interface_name text,
    trans_name     text,
    jrn_no         text not null,
    method         text,
    url            text,
    ip             text,
    req_param      text,
    rsp_param      text,
    headers        text,
    msg_code       text,
    msg_inf        text,
    debug_desc     text,
    sequence       text not null
        primary key,
    update_by      text,
    update_time    text,
    request_id     text,
    trans_nme_dsc  text
);

alter table debug_log
    owner to coretsdapi;

create table enum_dict
(
    group_name   text not null,
    project_name text not null,
    module_name  text,
    enum_nme     text not null,
    enum_ref     text not null,
    enum_cd      text,
    enum_val     text not null,
    desc_cn      text,
    desc_en      text,
    db_name      text,
    app_name     text not null,
    update_by    text,
    update_time  text,
    primary key (group_name, project_name, app_name, enum_nme, enum_ref, enum_val)
);

alter table enum_dict
    owner to coretsdapi;

create table enum_dict_his
(
    group_name   text not null,
    project_name text not null,
    module_name  text,
    enum_nme     text not null,
    enum_ref     text not null,
    enum_cd      text,
    enum_val     text not null,
    desc_cn      text,
    desc_en      text,
    db_name      text,
    app_name     text not null,
    update_by    text,
    update_time  text,
    primary key (group_name, project_name, app_name, enum_nme, enum_ref, enum_val)
);

alter table enum_dict_his
    owner to coretsdapi;

create table exesql_record
(
    id          serial
        primary key,
    group_name  text,
    schema_nm   text,
    env_name    text,
    exe_date    text,
    exe_time    text,
    exe_sql     text,
    status      text,
    update_by   text,
    update_time text
);

alter table exesql_record
    owner to coretsdapi;

create table interface_data
(
    group_name       text not null,
    project_name     text not null,
    interface_name   text,
    trans_name       text not null,
    class_name       text not null,
    module_name      text,
    trans_comment_zh text,
    trans_comment_en text,
    trans_class      text,
    simple_name      text,
    request_json     text,
    response_json    text,
    request_package  text,
    response_package text,
    request_class    text,
    response_class   text,
    interface_url    text,
    method_url       text,
    update_by        text,
    update_time      text,
    app_name         text not null,
    lable_name       text,
    associat_entity  text,
    associat_enum    text,
    req_parent_class text,
    rsp_parent_class text,
    primary key (group_name, project_name, app_name, class_name, trans_name)
);

alter table interface_data
    owner to coretsdapi;

create table interface_data_his
(
    group_name       text not null,
    project_name     text not null,
    app_name         text not null,
    interface_name   text,
    trans_name       text not null,
    class_name       text not null,
    trans_comment_zh text,
    trans_comment_en text,
    interface_url    text,
    method_url       text,
    lable_name       text,
    primary key (group_name, project_name, app_name, class_name, trans_name)
);

alter table interface_data_his
    owner to coretsdapi;

create table base_dict
(
    name_snake    varchar(100) not null
        primary key,
    type          varchar(20),
    db_typ        varchar(20),
    default_value varchar(256),
    comment_cn    varchar(2046),
    length        varchar(20),
    status        varchar(5),
    update_by     varchar(128),
    update_time   varchar(256)
);

alter table base_dict
    owner to coretsdapi;

create table biz_dict
(
    group_name    varchar(40)  not null,
    project_name  varchar(60)  not null,
    app_name      varchar(20)  not null,
    name_camel    varchar(100) not null,
    name_snake    varchar(100),
    type          varchar(20),
    db_typ        varchar(20),
    enum_nme      varchar(100),
    enum_ref      varchar(256),
    not_null      varchar(10),
    default_value varchar(256),
    comment_cn    varchar(2046),
    comment_en    varchar(2046),
    length        varchar(20),
    status        varchar(5),
    update_by     varchar(128),
    update_time   varchar(256),
    primary key (group_name, project_name, app_name, name_camel)
);

alter table biz_dict
    owner to coretsdapi;

create table user_info
(
    user_id     varchar(40)  not null
        primary key,
    username    varchar(128) not null
        unique,
    password    varchar(40)  not null,
    real_name   varchar(128) not null,
    email       varchar(128),
    phone       varchar(60),
    roles       varchar(5)   not null,
    status      varchar(5)   not null,
    create_by   varchar(128),
    create_time varchar(256),
    update_by   varchar(128),
    update_time varchar(256),
    remark      varchar(256)
);

alter table user_info
    owner to coretsdapi;

create index idx_user_info_email
    on user_info (email);

create index idx_user_info_role
    on user_info (roles);

create index idx_user_info_username
    on user_info (username);

create table table_record
(
    id               varchar(60) not null
        primary key,
    group_name       varchar(40) not null,
    project_name     varchar(60) not null,
    app_name         varchar(20) not null,
    module_name      varchar(20),
    table_name_camel varchar(100),
    table_name_snake varchar(100),
    table_comment_cn varchar(2046),
    table_comment_en varchar(2046),
    fields_json      text,
    primary_key_json text,
    indexes_json     text,
    lable_name       varchar(100),
    associat_enum    text,
    parent_class     varchar(256),
    gener_cd_flg     varchar(5),
    create_tab_flg   varchar(5),
    def_order_by     varchar(400),
    execsql_json     text,
    status           varchar(5),
    update_by        varchar(128),
    update_time      varchar(256)
);

alter table table_record
    owner to coretsdapi;

create index idx_table_record_app_name
    on table_record (app_name);

create index idx_table_record_group_name
    on table_record (group_name);

create index idx_table_record_group_project
    on table_record (group_name, project_name);

create index idx_table_record_project_app
    on table_record (project_name, app_name);

create index idx_table_record_project_name
    on table_record (project_name);

create index idx_table_record_status
    on table_record (status);

create index idx_table_record_table_name_snake
    on table_record (table_name_snake);

create index idx_table_record_update_time
    on table_record (update_time);

create table table_diff
(
    group_name       varchar(40)  not null,
    project_name     varchar(60)  not null,
    app_name         varchar(20)  not null,
    table_name_camel varchar(100) not null,
    table_name_snake varchar(100),
    schema_nm        varchar(60)  not null,
    compare_env      varchar(20)  not null,
    diff_type        varchar(20),
    local_value      text,
    remote_value     text,
    diff_field_count integer default 0,
    update_by        varchar(128),
    update_time      varchar(256),
    primary key (group_name, project_name, table_name_camel, schema_nm, compare_env)
);

alter table table_diff
    owner to coretsdapi;

create index idx_table_diff_app_name
    on table_diff (app_name);

create index idx_table_diff_diff_type
    on table_diff (diff_type);

create index idx_table_diff_table_snake
    on table_diff (table_name_snake);

create index idx_table_diff_update_time
    on table_diff (update_time);

create table table_data
(
    group_name       varchar(40)  not null,
    project_name     varchar(60)  not null,
    app_name         varchar(20)  not null,
    module_name      varchar(20),
    table_name_camel varchar(100) not null,
    table_name_snake varchar(100),
    table_comment_cn varchar(2046),
    table_comment_en varchar(2046),
    fields_json      text,
    primary_key_json text,
    indexes_json     text,
    lable_name       varchar(100),
    associat_enum    text,
    parent_class     varchar(256),
    gener_cd_flg     varchar(5),
    create_tab_flg   varchar(5),
    def_order_by     varchar(400),
    status           varchar(5),
    update_by        varchar(128),
    update_time      varchar(256),
    primary key (group_name, project_name, app_name, table_name_camel)
);

alter table table_data
    owner to coretsdapi;

create index idx1_table_data
    on table_data (group_name, project_name, app_name);

create table server_info
(
    group_name   varchar(40)  not null,
    project_name varchar(60)  not null,
    app_name     varchar(20)  not null,
    env_name     varchar(20)  not null,
    app_port     varchar(10)  not null,
    ip           varchar(25)  not null,
    port         varchar(10)  not null,
    username     varchar(60)  not null,
    password     varchar(60)  not null,
    app_path     varchar(256) not null,
    app_prop     varchar(256) not null,
    update_by    varchar(128),
    update_time  varchar(256),
    scan_flg     integer default 1,
    primary key (group_name, project_name, app_name, ip)
);

alter table server_info
    owner to coretsdapi;

create table project_setting
(
    group_name   varchar(40) not null,
    project_name varchar(60) not null,
    project_type varchar(10),
    project_desc varchar(512),
    app_name     varchar(20) not null,
    app_port     varchar(10),
    schema_nm    varchar(60),
    base_path    varchar(256),
    prop_path    varchar(256),
    enum_path    varchar(256),
    msgcd_path   varchar(256),
    cur_flag     varchar(5),
    show_flag    varchar(5),
    update_by    varchar(128),
    update_time  varchar(256),
    primary key (group_name, project_name, app_name)
);

alter table project_setting
    owner to coretsdapi;

create table project_group
(
    group_name varchar(40) not null
        primary key,
    group_desc varchar(256),
    cur_flag   varchar(5)
);

alter table project_group
    owner to coretsdapi;

create table project_folder
(
    group_name   varchar(40) not null,
    project_name varchar(60) not null,
    app_name     varchar(20) not null,
    module_name  varchar(50) not null,
    dir_base     varchar(256),
    dir_type     varchar(20) not null,
    dir_path     varchar(256),
    main_flg     varchar(5),
    update_by    varchar(128),
    update_time  varchar(256),
    primary key (group_name, project_name, module_name, dir_type)
);

alter table project_folder
    owner to coretsdapi;

create table user_proj_group
(
    group_name varchar(40)  not null,
    group_desc varchar(256),
    user_id    varchar(40)  not null,
    username   varchar(128) not null,
    cur_flag   varchar(5),
    primary key (group_name, user_id, username)
);

alter table user_proj_group
    owner to coretsdapi;

create table user_proj_setting
(
    group_name   varchar(40)  not null,
    project_name varchar(60)  not null,
    project_type varchar(10),
    project_desc varchar(512),
    app_name     varchar(20)  not null,
    app_port     varchar(10),
    user_id      varchar(40)  not null,
    username     varchar(128) not null,
    schema_nm    varchar(60),
    base_path    varchar(256),
    prop_path    varchar(256),
    enum_path    varchar(256),
    msgcd_path   varchar(256),
    cur_flag     varchar(5),
    show_flag    varchar(5),
    update_by    varchar(128),
    update_time  varchar(256),
    primary key (group_name, project_name, app_name, user_id, username)
);

alter table user_proj_setting
    owner to coretsdapi;

create table role_menu
(
    role_code   varchar(5)  not null,
    menu_key    varchar(64) not null,
    menu_name   varchar(64),
    group_key   varchar(64),
    group_name  varchar(64),
    sort_no     integer,
    show_flag   varchar(5) default 'Y',
    update_by   varchar(128),
    update_time varchar(256),
    primary key (role_code, menu_key)
);

alter table role_menu
    owner to coretsdapi;

create index idx_role_menu_role
    on role_menu (role_code);

