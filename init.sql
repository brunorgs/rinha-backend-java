CREATE TABLE public.payment (
	amount numeric(38, 2) NULL,
	fallback bool NULL,
	requested_at timestamptz(6) NULL,
	id uuid NOT NULL,
	correlation_id varchar(255) NULL,
	CONSTRAINT payment_pkey PRIMARY KEY (id)
);

-- Índices para melhorar performance
CREATE INDEX idx_payment_fallback ON payment(fallback);
CREATE INDEX idx_payment_requested_at ON payment(requested_at);