-- 演示分类树：动物界 / 植物界各一条到种的链路，便于联调

INSERT INTO taxon (id, parent_id, taxon_rank, scientific_name, materialized_path, child_count, is_accepted) VALUES
(1, NULL, 'KINGDOM', 'Animalia', '/1/', 1, 1),
(2, NULL, 'KINGDOM', 'Plantae', '/2/', 1, 1),
(3, 1, 'PHYLUM', 'Chordata', '/1/3/', 1, 1),
(4, 3, 'CLASS', 'Mammalia', '/1/3/4/', 1, 1),
(5, 4, 'ORDER', 'Primates', '/1/3/4/5/', 1, 1),
(6, 5, 'FAMILY', 'Hominidae', '/1/3/4/5/6/', 1, 1),
(7, 6, 'GENUS', 'Homo', '/1/3/4/5/6/7/', 1, 1),
(8, 7, 'SPECIES', 'Homo sapiens', '/1/3/4/5/6/7/8/', 0, 1),
(9, 2, 'PHYLUM', 'Magnoliophyta', '/2/9/', 1, 1),
(10, 9, 'CLASS', 'Magnoliopsida', '/2/9/10/', 1, 1),
(11, 10, 'ORDER', 'Rosales', '/2/9/10/11/', 1, 1),
(12, 11, 'FAMILY', 'Rosaceae', '/2/9/10/11/12/', 1, 1),
(13, 12, 'GENUS', 'Malus', '/2/9/10/11/12/13/', 1, 1),
(14, 13, 'SPECIES', 'Malus domestica', '/2/9/10/11/12/13/14/', 0, 1);

INSERT INTO taxon_i18n (taxon_id, locale, common_name, summary, description) VALUES
(1, 'zh-CN', '动物界', '多细胞异养真核生物。', '动物界涵盖从简单无脊椎动物到复杂脊椎动物的庞大类群。'),
(1, 'en', 'Animals', 'Multicellular heterotrophic eukaryotes.', 'Animals range from simple invertebrates to complex vertebrates.'),
(2, 'zh-CN', '植物界', '营光合作用的陆生与水生植物。', '植物界包括苔藓、蕨类、裸子植物与被子植物等。'),
(2, 'en', 'Plants', 'Photosynthetic land and aquatic plants.', 'Includes mosses, ferns, gymnosperms and angiosperms.'),
(3, 'zh-CN', '脊索动物门', '具脊索的动物。', NULL),
(3, 'en', 'Chordates', 'Animals with a notochord.', NULL),
(4, 'zh-CN', '哺乳纲', '恒温、胎生、哺乳的脊椎动物。', NULL),
(4, 'en', 'Mammals', 'Warm-blooded vertebrates that nurse young.', NULL),
(5, 'zh-CN', '灵长目', NULL, NULL),
(5, 'en', 'Primates', NULL, NULL),
(6, 'zh-CN', '人科', NULL, NULL),
(6, 'en', 'Great apes', NULL, NULL),
(7, 'zh-CN', '人属', NULL, NULL),
(7, 'en', 'Homo', NULL, NULL),
(8, 'zh-CN', '智人', '现代人类。', '智人是目前唯一存活的人属物种。'),
(8, 'en', 'Human', 'Modern humans.', 'Homo sapiens is the only extant species of Homo.'),
(9, 'zh-CN', '被子植物门', NULL, NULL),
(9, 'en', 'Flowering plants', NULL, NULL),
(10, 'zh-CN', '双子叶植物纲', NULL, NULL),
(10, 'en', 'Dicots', NULL, NULL),
(11, 'zh-CN', '蔷薇目', NULL, NULL),
(11, 'en', 'Rosales', NULL, NULL),
(12, 'zh-CN', '蔷薇科', NULL, NULL),
(12, 'en', 'Rose family', NULL, NULL),
(13, 'zh-CN', '苹果属', NULL, NULL),
(13, 'en', 'Malus', NULL, NULL),
(14, 'zh-CN', '苹果', '常见栽培果树。', '苹果是温带广泛栽培的果树物种。'),
(14, 'en', 'Apple', 'Common cultivated fruit tree.', 'Widely cultivated temperate fruit tree.');

ALTER TABLE taxon AUTO_INCREMENT = 15;
