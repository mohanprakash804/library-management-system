-- Optional sample data. Run once after schema.sql:
--   mysql -u root -p < sql/sample_data.sql
USE library_db;

INSERT INTO books (title, author, category, total_copies, available_copies) VALUES
 ('Core Java Volume I',            'Cay S. Horstmann',  'Programming', 3, 3),
 ('Effective Java',                'Joshua Bloch',      'Programming', 2, 2),
 ('Head First SQL',                'Lynn Beighley',     'Database',    2, 2),
 ('Database System Concepts',      'Abraham Silberschatz', 'Database', 2, 2),
 ('Digital Design',                'M. Morris Mano',    'Electronics', 3, 3),
 ('Microprocessor Architecture',   'Ramesh Gaonkar',    'Electronics', 2, 2),
 ('Wings of Fire',                 'A. P. J. Abdul Kalam', 'Biography', 2, 2),
 ('Clean Code',                    'Robert C. Martin',  'Programming', 1, 1);

INSERT INTO members (name, email, phone) VALUES
 ('Arun Kumar', 'arun@example.com', '9000000001'),
 ('Divya S',    'divya@example.com', '9000000002'),
 ('Karthik R',  'karthik@example.com', '9000000003');
