CREATE TABLE public.payment (
	amount numeric(38, 2) NULL,
	fallback bool NULL,
	requested_at timestamptz(6) NULL,
	id SERIAL PRIMARY KEY
);

CREATE INDEX idx_payment_requested_at ON payment(requested_at);