import type { ReactNode } from "react";

interface Props {
  title: string;
  children: ReactNode;
}

export default function PageWrapper({ title: _title, children }: Props) {
  return <div className="page-container">{children}</div>;
}
