-- Banks
INSERT INTO banks (bank_code, bank_name, logo_url, status) VALUES
('VCB', 'Vietcombank', 'https://upload.wikimedia.org/wikipedia/commons/e/e3/Vietcombank_logo_fixed.svg', 'active'),
('TCB', 'Techcombank', 'https://upload.wikimedia.org/wikipedia/commons/7/7c/Techcombank_logo.png?20170205112755', 'active'),
('ACB', 'ACB', 'https://acb.com.vn/_next/image?url=%2Fimages%2Flogo.svg&w=384&q=70', 'active'),
('BID', 'BIDV', 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Logo_BIDV.svg/1024px-Logo_BIDV.svg.png?20220815121139', 'active'),
('VPB', 'VPBank', 'https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/VPBank_logo.svg/1412px-VPBank_logo.svg.png?20210620225035', 'active')
ON CONFLICT (bank_code) DO NOTHING;

-- Branches for Vietcombank (VCB)
INSERT INTO branches (bank_code, branch_name, address, city, status) VALUES
('VCB', 'Chi nhánh Hà Nội', '191 Bà Triệu, Hai Bà Trưng', 'Hà Nội', 'active'),
('VCB', 'Chi nhánh TP.HCM', '29 Nguyễn Du, Quận 1', 'TP.HCM', 'active'),
('VCB', 'Chi nhánh Đà Nẵng', '1 Lê Duẩn, Hải Châu', 'Đà Nẵng', 'active')
ON CONFLICT DO NOTHING;

-- Branches for Techcombank (TCB)
INSERT INTO branches (bank_code, branch_name, address, city, status) VALUES
('TCB', 'Chi nhánh Hà Nội', '24 Lý Thường Kiệt, Hoàn Kiếm', 'Hà Nội', 'active'),
('TCB', 'Chi nhánh TP.HCM', '135 Nam Kỳ Khởi Nghĩa, Quận 3', 'TP.HCM', 'active'),
('TCB', 'Chi nhánh Hải Phòng', '10 Lạch Tray, Ngô Quyền', 'Hải Phòng', 'active')
ON CONFLICT DO NOTHING;

-- Branches for ACB
INSERT INTO branches (bank_code, branch_name, address, city, status) VALUES
('ACB', 'Chi nhánh Hà Nội', '442 Nguyễn Thị Minh Khai, Hai Bà Trưng', 'Hà Nội', 'active'),
('ACB', 'Chi nhánh TP.HCM', '456 Nguyễn Đình Chiểu, Quận 3', 'TP.HCM', 'active')
ON CONFLICT DO NOTHING;

-- Branches for BIDV
INSERT INTO branches (bank_code, branch_name, address, city, status) VALUES
('BID', 'Chi nhánh Hà Nội', '35 Hàng Vôi, Hoàn Kiếm', 'Hà Nội', 'active'),
('BID', 'Chi nhánh TP.HCM', '123 Nguyễn Du, Quận 1', 'TP.HCM', 'active')
ON CONFLICT DO NOTHING;

-- Branches for VPBank
INSERT INTO branches (bank_code, branch_name, address, city, status) VALUES
('VPB', 'Chi nhánh Hà Nội', '89 Láng Hạ, Đống Đa', 'Hà Nội', 'active'),
('VPB', 'Chi nhánh TP.HCM', '89 Tôn Thất Tùng, Quận 1', 'TP.HCM', 'active')
ON CONFLICT DO NOTHING;

-- Products
INSERT INTO product_catalog (product_name, description, category, status) VALUES
('Tiết kiệm có kỳ hạn', 'Sản phẩm tiết kiệm với lãi suất cố định theo kỳ hạn', 'SAVINGS', 'active'),
('Tiết kiệm linh hoạt', 'Tiết kiệm không kỳ hạn, rút tiền bất cứ lúc nào', 'SAVINGS', 'active'),
('Vay tín chấp', 'Vay không cần tài sản thế chấp, xét duyệt nhanh', 'LOAN', 'active'),
('Vay thế chấp', 'Vay có tài sản đảm bảo, lãi suất ưu đãi', 'LOAN', 'active'),
('Thẻ tín dụng', 'Thẻ tín dụng với nhiều ưu đãi và tích điểm', 'CARD', 'active'),
('Thẻ ghi nợ', 'Thẻ ghi nợ quốc tế, thanh toán tiện lợi', 'CARD', 'active'),
('Bảo hiểm nhân thọ', 'Bảo hiểm bảo vệ tài chính cho gia đình', 'INSURANCE', 'active'),
('Đầu tư quỹ', 'Đầu tư vào các quỹ với lợi nhuận tiềm năng', 'INVESTMENT', 'active')
ON CONFLICT DO NOTHING;

