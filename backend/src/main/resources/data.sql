-- 演示数据（幂等：已存在则跳过）
INSERT INTO wms_warehouse (code, name, address, contact, phone, status, created_at, updated_at)
SELECT 'WH01', '上海中心仓', '上海市青浦区华新镇', '张伟', '13800000001', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM wms_warehouse WHERE code = 'WH01');

INSERT INTO wms_zone (warehouse_code, code, name, type, status, created_at, updated_at)
SELECT 'WH01', 'RCV', '收货区', 'RECEIVING', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_zone WHERE warehouse_code='WH01' AND code='RCV');
INSERT INTO wms_zone (warehouse_code, code, name, type, status, created_at, updated_at)
SELECT 'WH01', 'STA', '存储区A', 'STORAGE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_zone WHERE warehouse_code='WH01' AND code='STA');
INSERT INTO wms_zone (warehouse_code, code, name, type, status, created_at, updated_at)
SELECT 'WH01', 'PCK', '拣货区', 'PICKING', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_zone WHERE warehouse_code='WH01' AND code='PCK');
INSERT INTO wms_zone (warehouse_code, code, name, type, status, created_at, updated_at)
SELECT 'WH01', 'SHP', '发货区', 'SHIPPING', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_zone WHERE warehouse_code='WH01' AND code='SHP');
INSERT INTO wms_zone (warehouse_code, code, name, type, status, created_at, updated_at)
SELECT 'WH01', 'QC', '质检区', 'QC', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_zone WHERE warehouse_code='WH01' AND code='QC');

INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'RCV', 'RCV-01', 'STAGING_IN', NULL, NULL, NULL, NULL, 0, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='RCV-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'SHP', 'SHP-01', 'STAGING_OUT', NULL, NULL, NULL, NULL, 0, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='SHP-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'QC', 'QC-01', 'QC', NULL, NULL, NULL, NULL, 0, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='QC-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-01-01-01', 'STORAGE', 'A', '01', '01', '01', 10, FALSE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-01-01-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-01-01-02', 'STORAGE', 'A', '01', '01', '02', 20, FALSE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-01-01-02');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-01-02-01', 'STORAGE', 'A', '01', '02', '01', 30, FALSE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-01-02-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-01-02-02', 'STORAGE', 'B', '01', '02', '02', 40, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-01-02-02');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-02-01-01', 'STORAGE', 'B', '02', '01', '01', 50, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-02-01-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'STA', 'A-02-01-02', 'STORAGE', 'C', '02', '01', '02', 60, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='A-02-01-02');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'PCK', 'P-01-01', 'PICKING', 'A', '01', '01', '01', 5, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='P-01-01');
INSERT INTO wms_location (warehouse_code, zone_code, code, type, abc_class, aisle, bay, level, pick_seq, mix_sku, mix_lot, status, created_at, updated_at)
SELECT 'WH01', 'PCK', 'P-01-02', 'PICKING', 'A', '01', '02', '01', 6, TRUE, TRUE, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_location WHERE warehouse_code='WH01' AND code='P-01-02');

INSERT INTO wms_owner (code, name, contact, phone, address, status, created_at, updated_at)
SELECT 'OWN01', '华东电子有限公司', '李娜', '13900000001', '上海市浦东新区', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_owner WHERE code='OWN01');

INSERT INTO wms_item (owner_code, code, name, spec, unit, pack_qty, barcode, category, lot_control, shelf_life_days, abc_class, weight, volume, min_stock, max_stock, status, created_at, updated_at)
SELECT 'OWN01', 'SKU001', '无线鼠标 M1', '黑色', 'EA', 20, '6901234567890', '电脑配件', TRUE, 730, 'A', 0.12, 0.0005, 50, 2000, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_item WHERE owner_code='OWN01' AND code='SKU001');
INSERT INTO wms_item (owner_code, code, name, spec, unit, pack_qty, barcode, category, lot_control, shelf_life_days, abc_class, weight, volume, min_stock, max_stock, status, created_at, updated_at)
SELECT 'OWN01', 'SKU002', '机械键盘 K87', '白色 87键', 'EA', 10, '6901234567891', '电脑配件', TRUE, 730, 'A', 0.85, 0.003, 30, 1000, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_item WHERE owner_code='OWN01' AND code='SKU002');
INSERT INTO wms_item (owner_code, code, name, spec, unit, pack_qty, barcode, category, lot_control, shelf_life_days, abc_class, weight, volume, min_stock, max_stock, status, created_at, updated_at)
SELECT 'OWN01', 'SKU003', 'USB-C 数据线 1m', '1米', 'EA', 100, '6901234567892', '线材', FALSE, NULL, 'B', 0.03, 0.0001, 200, 5000, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_item WHERE owner_code='OWN01' AND code='SKU003');
INSERT INTO wms_item (owner_code, code, name, spec, unit, pack_qty, barcode, category, lot_control, shelf_life_days, abc_class, weight, volume, min_stock, max_stock, status, created_at, updated_at)
SELECT 'OWN01', 'SKU004', '27寸显示器', '2K 144Hz', 'EA', 1, '6901234567893', '显示设备', FALSE, NULL, 'C', 5.5, 0.05, 5, 200, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_item WHERE owner_code='OWN01' AND code='SKU004');

INSERT INTO wms_supplier (code, name, contact, phone, address, status, created_at, updated_at)
SELECT 'SUP01', '深圳精密电子厂', '王强', '13700000001', '深圳市宝安区', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_supplier WHERE code='SUP01');
INSERT INTO wms_supplier (code, name, contact, phone, address, status, created_at, updated_at)
SELECT 'SUP02', '东莞线材制造', '赵敏', '13700000002', '东莞市松山湖', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_supplier WHERE code='SUP02');

INSERT INTO wms_customer (code, name, contact, phone, address, status, created_at, updated_at)
SELECT 'CUS01', '京东华东仓', '刘洋', '13600000001', '江苏省昆山市', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_customer WHERE code='CUS01');
INSERT INTO wms_customer (code, name, contact, phone, address, status, created_at, updated_at)
SELECT 'CUS02', '苏宁易购南京店', '陈静', '13600000002', '江苏省南京市', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM wms_customer WHERE code='CUS02');
