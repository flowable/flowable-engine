namespace address {
  function ip(interfaceName?: string): string

  function ipv6(interfaceName?: string): string 

  function mac(callback: (err: Error, addr: string) => void): void
  function mac(interfaceName: string, callback: (err: Error, addr: string) => void): void
  
  function dns(callback: (err: Error, servers: string[]) => void): void
  function dns(filePath: string, callback: (err: Error, servers: string[]) => void): void
}

function address(callback: (err: Error, addr: { ip: string, ipv6: string, mac: string }) => void): void
function address(interfaceName: string, callback: (err: Error, addr: { ip: string, ipv6: string, mac: string }) => void): void

export = address
