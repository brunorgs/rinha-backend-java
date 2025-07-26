CREATE TABLE public.payment (
	amount numeric(4, 2) NULL,
	fallback bool NULL,
	requested_at timestamp NULL,
	id SERIAL PRIMARY KEY
);

CREATE INDEX idx_payment_requested_at ON payment(requested_at);