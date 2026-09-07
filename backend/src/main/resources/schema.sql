-- ===================== 基础数据 =====================
CREATE TABLE IF NOT EXISTS wms_warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(255),
  contact VARCHAR(64),
  phone VARCHAR(32),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_warehouse_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS wms_zone (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_zone_code UNIQUE (warehouse_code, code)
);

CREATE TABLE IF NOT EXISTS wms_location (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  zone_code VARCHAR(32) NOT NULL,
  code VARCHAR(32) NOT NULL,
  type VARCHAR(32) NOT NULL,
  abc_class VARCHAR(4),
  aisle VARCHAR(16),
  bay VARCHAR(16),
  level VARCHAR(16),
  max_weight DECIMAL(18,3),
  max_volume DECIMAL(18,3),
  pick_seq INT DEFAULT 0,
  mix_sku BOOLEAN DEFAULT TRUE,
  mix_lot BOOLEAN DEFAULT TRUE,
  status VARCHAR(16) DEFAULT 'AVAILABLE',
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_location_code UNIQUE (warehouse_code, code)
);

CREATE TABLE IF NOT EXISTS wms_owner (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_owner_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS wms_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_code VARCHAR(32) NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(16) DEFAULT 'EA',
  pack_qty DECIMAL(18,3),
  barcode VARCHAR(64),
  category VARCHAR(64),
  lot_control BOOLEAN DEFAULT FALSE,
  shelf_life_days INT,
  abc_class VARCHAR(4),
  weight DECIMAL(18,3),
  volume DECIMAL(18,3),
  min_stock DECIMAL(18,3),
  max_stock DECIMAL(18,3),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_item_code UNIQUE (owner_code, code)
);

CREATE TABLE IF NOT EXISTS wms_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_supplier_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS wms_customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_customer_code UNIQUE (code)
);

-- ===================== 库存 =====================
CREATE TABLE IF NOT EXISTS wms_inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  location_code VARCHAR(32) NOT NULL,
  owner_code VARCHAR(32) NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  lot_no VARCHAR(64) DEFAULT '',
  qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  allocated_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
  receive_date DATE,
  expiry_date DATE,
  ref_no VARCHAR(64) DEFAULT '',
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  INDEX idx_inv_item (warehouse_code, owner_code, item_code),
  INDEX idx_inv_loc (warehouse_code, location_code)
);

CREATE TABLE IF NOT EXISTS wms_inventory_txn (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  txn_type VARCHAR(16) NOT NULL,
  warehouse_code VARCHAR(32),
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  from_location VARCHAR(32),
  to_location VARCHAR(32),
  qty DECIMAL(18,3),
  ref_no VARCHAR(64),
  operator VARCHAR(64),
  remark VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wms_count_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  warehouse_code VARCHAR(32) NOT NULL,
  zone_code VARCHAR(32),
  status VARCHAR(16) NOT NULL,
  remark VARCHAR(255),
  line_count INT,
  diff_count INT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wms_count_line (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  count_id BIGINT NOT NULL,
  inventory_id BIGINT,
  location_code VARCHAR(32),
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  system_qty DECIMAL(18,3),
  count_qty DECIMAL(18,3),
  diff_qty DECIMAL(18,3),
  status VARCHAR(16),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- ===================== 入库 =====================
CREATE TABLE IF NOT EXISTS wms_asn (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  warehouse_code VARCHAR(32) NOT NULL,
  owner_code VARCHAR(32) NOT NULL,
  supplier_code VARCHAR(32),
  type VARCHAR(16) DEFAULT 'PURCHASE',
  status VARCHAR(16) NOT NULL,
  expected_date DATE,
  external_no VARCHAR(64),
  remark VARCHAR(255),
  total_qty DECIMAL(18,3),
  received_qty DECIMAL(18,3),
  putaway_qty DECIMAL(18,3),
  cross_dock_order_code VARCHAR(32),
  cross_dock_qty DECIMAL(18,3),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_asn_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS wms_asn_line (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asn_id BIGINT NOT NULL,
  line_no INT,
  item_code VARCHAR(64) NOT NULL,
  lot_no VARCHAR(64),
  expiry_date DATE,
  expected_qty DECIMAL(18,3) NOT NULL,
  received_qty DECIMAL(18,3) DEFAULT 0,
  putaway_qty DECIMAL(18,3) DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wms_putaway_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  asn_id BIGINT NOT NULL,
  asn_line_id BIGINT NOT NULL,
  asn_code VARCHAR(32),
  warehouse_code VARCHAR(32),
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  inventory_id BIGINT,
  from_location VARCHAR(32),
  suggest_location VARCHAR(32),
  to_location VARCHAR(32),
  qty DECIMAL(18,3),
  status VARCHAR(16),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- ===================== 出库 =====================
CREATE TABLE IF NOT EXISTS wms_ship_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  warehouse_code VARCHAR(32) NOT NULL,
  owner_code VARCHAR(32) NOT NULL,
  customer_code VARCHAR(32),
  type VARCHAR(16) DEFAULT 'SALES',
  status VARCHAR(16) NOT NULL,
  priority INT DEFAULT 5,
  expected_ship_date DATE,
  external_no VARCHAR(64),
  carrier VARCHAR(64),
  address VARCHAR(255),
  remark VARCHAR(255),
  total_qty DECIMAL(18,3),
  allocated_qty DECIMAL(18,3),
  picked_qty DECIMAL(18,3),
  shipped_qty DECIMAL(18,3),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_so_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS wms_ship_order_line (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  line_no INT,
  item_code VARCHAR(64) NOT NULL,
  lot_no VARCHAR(64),
  order_qty DECIMAL(18,3) NOT NULL,
  allocated_qty DECIMAL(18,3) DEFAULT 0,
  picked_qty DECIMAL(18,3) DEFAULT 0,
  shipped_qty DECIMAL(18,3) DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wms_pick_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  order_id BIGINT NOT NULL,
  order_line_id BIGINT NOT NULL,
  order_code VARCHAR(32),
  warehouse_code VARCHAR(32),
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  inventory_id BIGINT,
  from_location VARCHAR(32),
  to_location VARCHAR(32),
  qty DECIMAL(18,3),
  picked_qty DECIMAL(18,3),
  status VARCHAR(16),
  wave_id BIGINT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 波次 (Wave): 多张出库单合并成一次总拣 + 播种(按单分拨)
CREATE TABLE IF NOT EXISTS wms_wave (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  warehouse_code VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  order_count INT,
  total_qty DECIMAL(18,3),
  picked_qty DECIMAL(18,3),
  sowed_qty DECIMAL(18,3),
  remark VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT uk_wave_code UNIQUE (code)
);

-- 波次总拣任务: 同一库存记录(库位/物料/批次)上多张单的拣货任务合并
CREATE TABLE IF NOT EXISTS wms_wave_pick_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  wave_id BIGINT NOT NULL,
  wave_code VARCHAR(32),
  warehouse_code VARCHAR(32),
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  inventory_id BIGINT,
  from_location VARCHAR(32),
  to_location VARCHAR(32),
  qty DECIMAL(18,3),
  picked_qty DECIMAL(18,3),
  order_count INT,
  status VARCHAR(16),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 播种任务: 总拣完成后按出库单/物料分拨到播种位
CREATE TABLE IF NOT EXISTS wms_sow_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  wave_id BIGINT NOT NULL,
  wave_code VARCHAR(32),
  order_id BIGINT NOT NULL,
  order_code VARCHAR(32),
  slot_no INT,
  owner_code VARCHAR(32),
  item_code VARCHAR(64),
  lot_no VARCHAR(64),
  qty DECIMAL(18,3),
  sowed_qty DECIMAL(18,3),
  status VARCHAR(16),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
